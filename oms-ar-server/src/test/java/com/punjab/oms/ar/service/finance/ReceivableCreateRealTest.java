package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.ArApplication;
import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.dto.finance.ReceivableCreateRequest;
import com.punjab.oms.ar.domain.dto.finance.ReceivableCreateRequest.*;
import com.punjab.oms.ar.enums.AmountType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.file.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
/** 运行 scripts/test-mysql-create，使用隔离测试库，默认禁用。 @author Punjab */
@SpringBootTest(classes=ArApplication.class,webEnvironment=SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("pre")
@EnabledIfSystemProperty(named="ar.create.real",matches="true")
@EnabledIfSystemProperty(named="ar.create.real.commit",matches="true")
class ReceivableCreateRealTest {
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r){
  r.add("spring.datasource.url",()->"jdbc:mysql://127.0.0.1:13306/oms_ar_test");
  r.add("spring.datasource.username",()->"oms_ar");
  r.add("spring.datasource.password",()->System.getenv("AR_TEST_PASSWORD"));
  r.add("spring.cloud.service-registry.auto-registration.enabled",()->"false");
 }
 @Autowired ReceivableService service;
 @Autowired ReceivableQueryService queries;
 @Autowired JdbcTemplate jdbc;
 @BeforeEach void setup() throws Exception {
  assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("oms_ar_test");
  for(String file:List.of("V001__ar_core.sql","V002__ar_audit.sql","V011__ar_outbox.sql","V005__ar_adjustment_link.sql","V009__ar_exchange_basis.sql","V010__ar_source_guard.sql"))
   for(String sql:Files.readString(Path.of("../scratch/dbddl/"+file)).split(";")) if(!sql.isBlank())jdbc.execute(sql);
  CurrentUser.set(new CurrentUser.Identity("integration",Set.of(1L),Set.of("ar:create","ar:read")));
 }
 @AfterEach void clear(){CurrentUser.clear();}
 private String key(){return UUID.randomUUID().toString();}
 private Line line(String key,Long group,AmountType type,String amount,Source source){
  return new Line(key,group,type,new BigDecimal(amount),type==AmountType.OUTPUT_TAX?new BigDecimal("0.13"):null,
     type==AmountType.OUTPUT_TAX?new BigDecimal("100"):null,source);
 }
 private ReceivableCreateRequest request(String key,Line... lines){return new ReceivableCreateRequest(key,1L,1L,"CNY","SHIPMENT",LocalDate.of(2026,10,31),null,"test",List.of(lines));}
 @Test @DisplayName("价税生成即生效且明细来源和初始化快照齐全") void createAndReplay(){
  String event=key();
  var request=request(key(),line("A",null,AmountType.PRINCIPAL,"100",new Source("OMS","SHIPMENT","N","I",null,event,"price")),line("A",null,AmountType.OUTPUT_TAX,"13",new Source("OMS","SHIPMENT","N","I",null,event,"tax")));
  long id=service.create(request); assertThat(id).isPositive();
  assertThat(service.create(request)).isEqualTo(id);
  assertThat(jdbc.queryForObject("SELECT SUM(open_amount) FROM oms_ar_line WHERE document_id=?",BigDecimal.class,id)).isEqualByComparingTo("113");
  assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT group_id) FROM oms_ar_line WHERE document_id=?",Integer.class,id)).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_line_source s JOIN oms_ar_line l ON s.line_id=l.id WHERE l.document_id=?",Integer.class,id)).isEqualTo(2);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_lifecycle_change WHERE object_type='DOCUMENT' AND object_id=?",Integer.class,id)).isEqualTo(1);
 }
 @Test @DisplayName("同键不同内容拒绝") void conflict(){
  String key=key();service.create(request(key,line("A",null,AmountType.PRINCIPAL,"100",null)));
  assertThatThrownBy(()->service.create(request(key,line("A",null,AmountType.PRINCIPAL,"101",null)))).isInstanceOf(ServiceException.class);
 }
 @Test @DisplayName("来源冲突回滚整单、业务组及幂等占位") void rollbackSourceConflict(){
  String event=key();var source=new Source("OMS","SHIPMENT","N","I",null,event,"price");
  service.create(request(key(),line("A",null,AmountType.PRINCIPAL,"100",source)));
  int before=jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_document",Integer.class);
  int groupsBefore=jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_business_group",Integer.class);
  int eventsBefore=jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_lifecycle_event",Integer.class);
  String duplicate=key();
  assertThatThrownBy(()->service.create(request(duplicate,line("B",null,AmountType.PRINCIPAL,"100",source)))).isInstanceOf(RuntimeException.class);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_document",Integer.class)).isEqualTo(before);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_business_group",Integer.class)).isEqualTo(groupsBefore);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_lifecycle_event",Integer.class)).isEqualTo(eventsBefore);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,duplicate)).isZero();
 }
 @Test @DisplayName("已结清业务组可追加税额且不改原行") void appendTax(){
  long id=service.create(request(key(),line("A",null,AmountType.PRINCIPAL,"100",null)));
  Long group=jdbc.queryForObject("SELECT group_id FROM oms_ar_line WHERE document_id=?",Long.class,id);
  jdbc.update("UPDATE oms_ar_line SET settled_amount=100,open_amount=0 WHERE document_id=?",id);
  long tax=service.create(request(key(),line("A",group,AmountType.OUTPUT_TAX,"13",null)));
  assertThat(jdbc.queryForObject("SELECT group_id FROM oms_ar_line WHERE document_id=?",Long.class,tax)).isEqualTo(group);
  assertThat(jdbc.queryForObject("SELECT open_amount FROM oms_ar_line WHERE document_id=?",BigDecimal.class,id)).isEqualByComparingTo("0");
 }
 @Test @DisplayName("无身份不能生成") void noActor(){CurrentUser.clear();assertThatThrownBy(()->service.create(request(key(),line("A",null,AmountType.PRINCIPAL,"100",null)))).isInstanceOf(ServiceException.class);}

 @Test @DisplayName("并发同请求只生成一张单") void concurrentReplay() throws Exception {
  var input=request(key(),line("A",null,AmountType.PRINCIPAL,"100",null));
  try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
   var ready=new java.util.concurrent.CountDownLatch(2);
   java.util.concurrent.Callable<Long> call=()->{
    CurrentUser.set(new CurrentUser.Identity("integration",Set.of(1L),Set.of("ar:create","ar:read")));
    try{ready.countDown();if(!ready.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("start timeout");return service.create(input);}
    finally{CurrentUser.clear();}
   };
   var first=executor.submit(call);var second=executor.submit(call);
   long id=first.get(10,java.util.concurrent.TimeUnit.SECONDS);
   assertThat(second.get(10,java.util.concurrent.TimeUnit.SECONDS)).isEqualTo(id);
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_line WHERE document_id=?",Integer.class,id)).isEqualTo(1);
  }
 }
 @Test @DisplayName("越权和精度错误不写入数据") void rejectsBadInput(){
  var invalid=request(key(),line("A",null,AmountType.PRINCIPAL,"0.001",null));
  assertThatThrownBy(()->service.create(invalid)).isInstanceOf(ServiceException.class).hasMessage("金额超出币种精度");
  CurrentUser.set(new CurrentUser.Identity("integration",Set.of(2L),Set.of("ar:create","ar:read")));
  assertThatThrownBy(()->service.create(request(key(),line("A",null,AmountType.PRINCIPAL,"100",null)))).isInstanceOf(ServiceException.class);
 }
 @Test @DisplayName("未知业务组失败且幂等记录回滚") void missingGroup(){
  String key=key();
  assertThatThrownBy(()->service.create(request(key,line("A",Long.MAX_VALUE,AmountType.PRINCIPAL,"100",null)))).isInstanceOf(ServiceException.class);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,key)).isZero();
 }

 @Test @DisplayName("详情包含来源和价税分列汇总") void queryDetail(){
  long id=service.create(request(key(),line("A",null,AmountType.PRINCIPAL,"100",new Source("OMS","SHIPMENT","NO","ID","L1",key(),"price")),line("A",null,AmountType.OUTPUT_TAX,"13",null)));
  var d=queries.detail(new com.punjab.oms.ar.domain.query.finance.ReceivableDetailQuery(id,1L));
  assertThat(d).isNotNull();assertThat(d.lines()).hasSize(2);
  assertThat(d.principal().openAmount()).isEqualByComparingTo("100");assertThat(d.outputTax().openAmount()).isEqualByComparingTo("13");
  assertThat(d.lines().getFirst().source().sourceLineId()).isEqualTo("L1");assertThat(d.settlementStatus()).isEqualTo("UNSETTLED");
 }
 @Test @DisplayName("正负净额零不等于结清") void netZeroNotSettled(){
  long id=service.create(request(key(),line("A",null,AmountType.PRINCIPAL,"100",null),line("B",null,AmountType.PRINCIPAL,"-100",null)));
  var d=queries.detail(new com.punjab.oms.ar.domain.query.finance.ReceivableDetailQuery(id,1L));
  assertThat(d).isNotNull();assertThat(d.principal().openAmount()).isEqualByComparingTo("0");assertThat(d.settlementStatus()).isEqualTo("UNSETTLED");
 }
 @Test @DisplayName("分页总数和数据范围一致") void pages(){
  var q=new com.punjab.oms.ar.domain.query.finance.ReceivablePageQuery();q.setLegalEntityId(1L);q.setCustomerId(Long.MAX_VALUE);
  var result=queries.page(q);assertThat(result).isNotNull();assertThat(result.total()).isZero();assertThat(result.records()).isEmpty();
  q.setCustomerId(1L);q.setSize(1);result=queries.page(q);assertThat(result.records()).hasSize(1);
  assertThat(result.total()).isEqualTo(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_document WHERE legal_entity_id=1 AND customer_id=1 AND delete_status=false",Long.class));
  q.setPage(Integer.MAX_VALUE);assertThat(queries.page(q).records()).isEmpty();
 }
 @Test @DisplayName("不能通过ID跨主体读取") void queryScope(){
  long id=service.create(request(key(),line("A",null,AmountType.PRINCIPAL,"100",null)));
  CurrentUser.set(new CurrentUser.Identity("other",Set.of(2L),Set.of("ar:read")));
  assertThatThrownBy(()->queries.detail(new com.punjab.oms.ar.domain.query.finance.ReceivableDetailQuery(id,2L))).isInstanceOf(ServiceException.class);
 }

 @Test @DisplayName("逐行余额决定部分结算和结清") void settlementStates(){
  long id=service.create(request(key(),line("A",null,AmountType.PRINCIPAL,"100",null)));
  var q=new com.punjab.oms.ar.domain.query.finance.ReceivableDetailQuery(id,1L);
  jdbc.update("UPDATE oms_ar_line SET settled_amount=20,open_amount=80 WHERE document_id=?",id);
  assertThat(queries.detail(q).settlementStatus()).isEqualTo("PARTIAL");
  jdbc.update("UPDATE oms_ar_line SET settled_amount=100,open_amount=0 WHERE document_id=?",id);
  assertThat(queries.detail(q).settlementStatus()).isEqualTo("SETTLED");
 }
 @Test @DisplayName("逻辑删除与非法分页不可查询") void deletedAndInvalid(){
  long id=service.create(request(key(),line("A",null,AmountType.PRINCIPAL,"100",null)));
  jdbc.update("UPDATE oms_ar_document SET delete_status=true WHERE id=?",id);
  assertThatThrownBy(()->queries.detail(new com.punjab.oms.ar.domain.query.finance.ReceivableDetailQuery(id,1L))).isInstanceOf(ServiceException.class);
  var q=new com.punjab.oms.ar.domain.query.finance.ReceivablePageQuery();q.setLegalEntityId(1L);q.setSize(201);
  assertThatThrownBy(()->queries.page(q)).isInstanceOf(ServiceException.class).hasMessage("查询参数无效");
  q.setSize(20);q.setPage(0);assertThatThrownBy(()->queries.page(q)).isInstanceOf(ServiceException.class);
 }
 @Test @DisplayName("无来源调整详情仍返回原组及原行，独立负数无调整关联") void adjustmentDetail(){
  long original=service.create(request(key(),line("A",null,AmountType.PRINCIPAL,"100",null)));
  var originalRow=queries.detail(new com.punjab.oms.ar.domain.query.finance.ReceivableDetailQuery(original,1L)).lines().getFirst();
  long adjusted=service.create(request(key(),new Line("A",null,AmountType.PRINCIPAL,new BigDecimal("-20"),null,null,null,originalRow.id())));
  var detail=queries.detail(new com.punjab.oms.ar.domain.query.finance.ReceivableDetailQuery(adjusted,1L));
  var tree=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().valueToTree(detail);
  assertThat(tree.at("/lines/0/adjustment/originalLineId").asLong()).isEqualTo(originalRow.id());
  assertThat(tree.at("/lines/0/adjustment/relatedGroupId").asLong()).isEqualTo(originalRow.groupId());
  assertThat(detail.lines().getFirst().source()).isNull();
  long independent=service.create(request(key(),line("A",null,AmountType.PRINCIPAL,"-10",null)));
  var independentTree=new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().valueToTree(queries.detail(new com.punjab.oms.ar.domain.query.finance.ReceivableDetailQuery(independent,1L)));
  assertThat(independentTree.at("/lines/0/adjustment").isNull()).isTrue();
 }

}
