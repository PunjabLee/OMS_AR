package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.ArApplication;
import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.enums.AmountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;import java.time.LocalDate;import java.nio.file.*;import java.util.*;
import static org.assertj.core.api.Assertions.*;
/** scripts/test-mysql-outbox-enqueue：独立测试库，验证业务和通知原子保存。 @author Punjab */
@SpringBootTest(classes=ArApplication.class,webEnvironment=SpringBootTest.WebEnvironment.NONE) @ActiveProfiles("pre")
@EnabledIfSystemProperty(named="ar.outbox-enqueue.real",matches="true") @EnabledIfSystemProperty(named="ar.outbox-enqueue.real.commit",matches="true")
class OutboxEnqueueRealTest {
 @DynamicPropertySource static void config(DynamicPropertyRegistry p){p.add("spring.datasource.url",()->"jdbc:mysql://127.0.0.1:13306/oms_ar_test");p.add("spring.datasource.username",()->"oms_ar");p.add("spring.datasource.password",()->System.getenv("AR_TEST_PASSWORD"));}
 @Autowired OutboxLeaseService leases;@Autowired FinanceJournalService journal;@Autowired OutboxEnqueueService outbox;@Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 @Autowired JdbcTemplate jdbc;@Autowired ReceivableService receivables;@Autowired ReceiptService receipts;@Autowired ObjectMapper json;
 String actor;long customer;
 @BeforeEach void setup()throws Exception{
  assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("oms_ar_test");
  try(var files=Files.list(Path.of("../scratch/dbddl"))){for(var file:files.filter(p->p.getFileName().toString().matches("V[0-9]+__.*\\.sql")).sorted().toList())for(String sql:Files.readString(file).split(";"))if(!sql.isBlank())jdbc.execute(sql);}
  actor="outbox-"+UUID.randomUUID();customer=Math.abs(UUID.randomUUID().getMostSignificantBits()>>>1);
  CurrentUser.set(new CurrentUser.Identity(actor,Set.of(1L),Set.of("ar:create","ar:receipt:create")));
 }
 @AfterEach void clear(){CurrentUser.clear();}
 ReceivableCreateRequest ar(String key){return new ReceivableCreateRequest(key,customer,1L,"CNY","SHIPMENT",LocalDate.now(),null,"outbox-test",List.of(new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,new BigDecimal("100"),null,null,null),new ReceivableCreateRequest.Line("A",null,AmountType.OUTPUT_TAX,new BigDecimal("13"),new BigDecimal("0.13"),new BigDecimal("100"),null)));}
 ReceiptCreateRequest receipt(String key){return new ReceiptCreateRequest(key,customer,1L,"CNY",new BigDecimal("113"),LocalDate.now(),"TEST",key,key);}
 void verifyPayload(String eventType)throws Exception{
  var rows=jdbc.queryForList("SELECT o.*,e.id AS expected_event FROM oms_ar_outbox o JOIN oms_ar_lifecycle_event e ON e.id=o.event_id WHERE e.actor=?",actor);
  assertThat(rows).hasSize(1);var row=rows.getFirst();long event=((Number)row.get("event_id")).longValue();
  assertThat(row.get("status")).isEqualTo("PENDING");assertThat(((Number)row.get("legal_entity_id")).longValue()).isEqualTo(1);assertThat(row.get("destination")).isEqualTo("FINANCE_EVENTS");
  var payload=json.readTree(row.get("payload").toString());assertThat(payload.path("schemaVersion").asInt()).isEqualTo(1);assertThat(payload.path("eventId").asLong()).isEqualTo(event);assertThat(payload.path("legalEntityId").asLong()).isEqualTo(1);assertThat(payload.at("/event/eventType").asText()).isEqualTo(eventType);
  var changes=jdbc.queryForList("SELECT * FROM oms_ar_lifecycle_change WHERE event_id=? ORDER BY id",event);assertThat(payload.path("changes").size()).isEqualTo(changes.size());assertThat(changes).isNotEmpty();
  for(int i=0;i<changes.size();i++){var actual=payload.path("changes").get(i);var stored=changes.get(i);assertThat(actual.path("id").asLong()).isEqualTo(((Number)stored.get("id")).longValue());assertThat(actual.path("afterSnapshot")).isEqualTo(json.readTree(stored.get("after_snapshot").toString()));assertThat(actual.path("beforeSnapshot").isNull()).isTrue();assertThat(actual.path("deltaSnapshot").isObject()).isTrue();}
 }
 @Test @DisplayName("应收创建价税快照完整入队，重放不重复")void creation()throws Exception{var request=ar(UUID.randomUUID().toString());long id=receivables.create(request);assertThat(receivables.create(request)).isEqualTo(id);verifyPayload("CREATED");}
 @Test @DisplayName("统一流水收款入队，重放不重复")void receipt()throws Exception{var request=receipt(UUID.randomUUID().toString());long id=receipts.create(request);assertThat(receipts.create(request)).isEqualTo(id);verifyPayload("RECEIPT_CREATED");}
 @Test @DisplayName("应收入队失败回滚所有业务与流水，可重试")void creationFailure(){failure(true);}
 @Test @DisplayName("统一流水入队失败回滚收款，可重试")void receiptFailure(){failure(false);}
 void failure(boolean creation){
  String key=UUID.randomUUID().toString();String trigger="test_outbox_"+key.replace("-","");
  jdbc.execute("ALTER TABLE oms_ar_outbox ADD CONSTRAINT "+trigger+" CHECK(created_by<>'"+actor+"')");
  try{assertThatThrownBy(()->{if(creation)receivables.create(ar(key));else receipts.create(receipt(key));}).isInstanceOf(org.springframework.dao.DataAccessException.class);
   for(String table:List.of("oms_ar_document","oms_ar_line","oms_ar_business_group","oms_ar_receipt","oms_ar_operation_request","oms_ar_lifecycle_event","oms_ar_lifecycle_change","oms_ar_outbox"))assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE created_by=?",Integer.class,actor)).as(table).isZero();
  }finally{jdbc.execute("ALTER TABLE oms_ar_outbox DROP CHECK "+trigger);}
  if(creation)receivables.create(ar(key));else receipts.create(receipt(key));assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_outbox WHERE created_by=?",Integer.class,actor)).isEqualTo(1);
 }
 @Test @DisplayName("通知服务禁止脱离业务事务保存")void mandatory(){assertThatThrownBy(()->outbox.enqueue(1L,1L,actor)).isInstanceOf(org.springframework.transaction.IllegalTransactionStateException.class);}
 @Test @DisplayName("完整变更通知保留精确金额、税率、版本与主体")void changes()throws Exception{
  CurrentUser.set(new CurrentUser.Identity(actor,Set.of(2L),Set.of("ar:create")));
  String before="{\"amount\":9999999999999999.12345678,\"taxRate\":0.13000000}";
  String after="{\"amount\":9999999999999998.12345678,\"taxRate\":0.13000000}";
  long event=new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status->{
   var ticket=journal.begin(new JournalBeginRequest(2L,"ar:create","OUTBOX_TEST",UUID.randomUUID().toString(),"{}"));
   long id=journal.append(new JournalEventRequest("TEST_CHANGE",actor,"test","TEST","1",UUID.randomUUID().toString(),null,List.of(new JournalEventRequest.Change("TEST",1L,before,"{\"amount\":-1}",after,4L,5L))),ticket);journal.complete(ticket,id);return id;
  });
  String payload=jdbc.queryForObject("SELECT payload FROM oms_ar_outbox WHERE event_id=?",String.class,event);
  var tree=json.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(payload);
  assertThat(tree.path("legalEntityId").asLong()).isEqualTo(2);
  assertThat(tree.at("/event/schemaVersion").asInt()).isEqualTo(2);
  assertThat(tree.at("/changes/0/beforeSnapshot/amount").isTextual()).isTrue();
  assertThat(new BigDecimal(tree.at("/changes/0/beforeSnapshot/amount").asText())).isEqualByComparingTo("9999999999999999.12345678");
  assertThat(new BigDecimal(tree.at("/changes/0/afterSnapshot/amount").asText())).isEqualByComparingTo("9999999999999998.12345678");
  assertThat(new BigDecimal(tree.at("/changes/0/afterSnapshot/taxRate").asText())).isEqualByComparingTo("0.13");
  assertThat(tree.at("/changes/0/deltaSnapshot/amount").asInt()).isEqualTo(-1);
  assertThat(tree.at("/changes/0/beforeVersion").asLong()).isEqualTo(4);assertThat(tree.at("/changes/0/afterVersion").asLong()).isEqualTo(5);
 }

 @Test @DisplayName("应收初始化金额在数据库快照和通知中保持精确")void creationPrecision()throws Exception{
  var input=ar(UUID.randomUUID().toString());var amount=new BigDecimal("9999999999999999.12");
  long document=receivables.create(new ReceivableCreateRequest(input.requestKey(),customer,1L,"CNY","SHIPMENT",LocalDate.now(),null,"precision",List.of(new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,amount,null,null,null))));
  var data=jdbc.queryForMap("SELECT c.after_snapshot,o.payload,JSON_VALUE(c.after_snapshot,'$.receivableAmount' RETURNING DECIMAL(24,8)) AS exact_amount FROM oms_ar_line l JOIN oms_ar_lifecycle_change c ON c.object_id=l.id AND c.object_type='LINE' JOIN oms_ar_outbox o ON o.event_id=c.event_id WHERE l.document_id=?",document);
  assertThat((BigDecimal)data.get("exact_amount")).isEqualByComparingTo(amount);
  assertThat(new BigDecimal(json.readTree(data.get("after_snapshot").toString()).path("receivableAmount").asText())).isEqualByComparingTo(amount);
  var changes=json.readTree(data.get("payload").toString()).path("changes");boolean found=false;
  for(var change:changes)if(change.path("objectType").asText().equals("LINE")){assertThat(new BigDecimal(change.at("/afterSnapshot/receivableAmount").asText())).isEqualByComparingTo(amount);found=true;}
  assertThat(found).isTrue();
 }

 @Test @DisplayName("业务生成通知立即可领取，不受应用和数据库时区差异影响")void immediatelyReady(){
  long scope=customer;CurrentUser.set(new CurrentUser.Identity(actor,Set.of(scope),Set.of("ar:create")));
  var input=ar(UUID.randomUUID().toString());receivables.create(new ReceivableCreateRequest(input.requestKey(),customer,scope,input.currency(),input.businessType(),input.businessDate(),null,"ready",input.lines()));
  long id=jdbc.queryForObject("SELECT id FROM oms_ar_outbox WHERE created_by=?",Long.class,actor);
  assertThat(jdbc.queryForObject("SELECT next_attempt_at<=NOW(6) FROM oms_ar_outbox WHERE id=?",Boolean.class,id)).isTrue();
  var claimed=leases.claim(new OutboxClaimRequest(scope,UUID.randomUUID().toString(),3,30));assertThat(claimed).isNotNull();assertThat(claimed.id()).isEqualTo(id);
 }

}
