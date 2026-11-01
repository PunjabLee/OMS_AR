package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.ArApplication;import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.enums.*;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;import java.time.LocalDate;import java.nio.file.*;import java.util.*;
import static org.assertj.core.api.Assertions.*;
/** scripts/test-mysql-exchange：汇率差异来源与实际应收验证。 @author Punjab */
@SpringBootTest(classes=ArApplication.class,webEnvironment=SpringBootTest.WebEnvironment.NONE) @ActiveProfiles("pre")
@EnabledIfSystemProperty(named="ar.exchange.real",matches="true") @EnabledIfSystemProperty(named="ar.exchange.real.commit",matches="true")
class ExchangeBasisRealTest {
 @DynamicPropertySource static void config(DynamicPropertyRegistry p){p.add("spring.datasource.url",()->"jdbc:mysql://127.0.0.1:13306/oms_ar_test");p.add("spring.datasource.username",()->"oms_ar");p.add("spring.datasource.password",()->System.getenv("AR_TEST_PASSWORD"));}
 @Autowired com.fasterxml.jackson.databind.ObjectMapper json;@Autowired ReceiptService receipts;@Autowired ReceivableQueryService queries;@Autowired ReceivableService receivables;@Autowired SettlementService settlements;@Autowired JdbcTemplate jdbc;
 private String key(){return UUID.randomUUID().toString();} private BigDecimal n(String v){return new BigDecimal(v);}
 private void actor(){CurrentUser.set(new CurrentUser.Identity("offset-test",Set.of(1L),Set.of("ar:create","ar:read","ar:settle","ar:reverse","ar:receipt:create")));}
 @BeforeEach void setup()throws Exception{assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("oms_ar_test");
  for(String name:List.of("V001__ar_core.sql","V002__ar_audit.sql","V003__ar_receipt.sql","V004__ar_settlement.sql","V005__ar_adjustment_link.sql","V009__ar_exchange_basis.sql")){var file=Path.of("../scratch/dbddl/"+name);if(Files.exists(file))for(String sql:Files.readString(file).split(";"))if(!sql.isBlank())jdbc.execute(sql);}actor();}
 @AfterEach void clear(){CurrentUser.clear();}
 private long line(AmountType type,String amount,Long group,Long original){
  long doc=receivables.create(new ReceivableCreateRequest(key(),1L,1L,"CNY","TAX_ADJUSTMENT",LocalDate.now(),null,"test",List.of(new ReceivableCreateRequest.Line("A",group,type,n(amount),type==AmountType.OUTPUT_TAX?n("0.13"):null,type==AmountType.OUTPUT_TAX?n("100"):null,null,original))));
  return jdbc.queryForObject("SELECT id FROM oms_ar_line WHERE document_id=?",Long.class,doc);
 }
 private long group(long line){return jdbc.queryForObject("SELECT group_id FROM oms_ar_line WHERE id=?",Long.class,line);}
 private BigDecimal open(long line){return jdbc.queryForObject("SELECT open_amount FROM oms_ar_line WHERE id=?",BigDecimal.class,line);}

 private long fx(String amount,ReceivableCreateRequest.ExchangeBasis basis){return receivables.create(new ReceivableCreateRequest(key(),1L,1L,"CNY","EXCHANGE_DIFFERENCE",LocalDate.now(),null,"fx",List.of(new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,n(amount),null,null,null,null,basis))));}
 private ReceivableCreateRequest.ExchangeBasis basis(String original,String oldRate,String newRate,ExchangeRateDirection direction){return new ReceivableCreateRequest.ExchangeBasis("USD",n(original),n(oldRate),n(newRate),direction);}
 @Test @DisplayName("直报价差额作为结算币种实际应收且保存依据")void direct(){long doc=fx("20",basis("100","7.1","7.3",ExchangeRateDirection.SETTLEMENT_PER_ORIGINAL));long line=jdbc.queryForObject("SELECT id FROM oms_ar_line WHERE document_id=?",Long.class,doc);assertThat(open(line)).isEqualByComparingTo("20");assertThat(jdbc.queryForObject("SELECT confirmed_difference FROM oms_ar_exchange_basis WHERE line_id=?",BigDecimal.class,line)).isEqualByComparingTo("20");assertThat(jdbc.queryForObject("SELECT old_settlement_amount FROM oms_ar_exchange_basis WHERE line_id=?",BigDecimal.class,line)).isEqualByComparingTo("710");}
 @Test @DisplayName("反报价先分别按结算币种舍入再求差")void inverse(){long doc=fx("-1.66",basis("10","3","6",ExchangeRateDirection.ORIGINAL_PER_SETTLEMENT));long line=jdbc.queryForObject("SELECT id FROM oms_ar_line WHERE document_id=?",Long.class,doc);assertThat(open(line)).isEqualByComparingTo("-1.66");assertThat(jdbc.queryForObject("SELECT new_settlement_amount FROM oms_ar_exchange_basis WHERE line_id=?",BigDecimal.class,line)).isEqualByComparingTo("1.67");}
 @Test @DisplayName("缺少依据或差额不符不能生成应收")void invalid(){assertThatThrownBy(()->fx("20",null)).isInstanceOf(ServiceException.class);assertThatThrownBy(()->fx("21",basis("100","7.1","7.3",ExchangeRateDirection.SETTLEMENT_PER_ORIGINAL))).isInstanceOf(ServiceException.class);}

 @Test @DisplayName("汇率应收可正常核销，依据快照和详情版本可追溯")void settleAndRead(){
  long doc=fx("20",basis("100","7.1","7.3",ExchangeRateDirection.SETTLEMENT_PER_ORIGINAL));var query=new com.punjab.oms.ar.domain.query.finance.ReceivableDetailQuery(doc,1L);var before=queries.detail(query).lines().getFirst();assertThat(before.version()).isZero();assertThat(before.exchangeBasis().originalCurrency()).isEqualTo("USD");assertThat(before.exchangeBasis().confirmedDifference()).isEqualByComparingTo("20");
  long receipt=receipts.create(new ReceiptCreateRequest(key(),1L,1L,"CNY",n("20"),LocalDate.now(),"TEST",key(),"FX"));var request=new SettlementPreviewRequest(1L,receipt,n("20"),List.of(before.groupId()),AllocationStrategy.GROUP_TAX_FIRST);var preview=settlements.preview(request);settlements.confirm(new SettlementConfirmRequest(key(),request,preview.fingerprint(),"settle fx"));
  var after=queries.detail(query).lines().getFirst();assertThat(after.openAmount()).isEqualByComparingTo("0");assertThat(after.version()).isEqualTo(1L);assertThat(after.exchangeBasis()).isEqualTo(before.exchangeBasis());
  long basisId=jdbc.queryForObject("SELECT id FROM oms_ar_exchange_basis WHERE line_id=?",Long.class,before.id());assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_lifecycle_change WHERE object_type='EXCHANGE_BASIS' AND object_id=? AND before_snapshot IS NULL",Integer.class,basisId)).isEqualTo(1);
 }
 @Test @DisplayName("来源精度、非法汇率和零差额被拒绝")void invalidBasis(){
  assertThatThrownBy(()->fx("1",basis("1.001","7","8",ExchangeRateDirection.SETTLEMENT_PER_ORIGINAL))).isInstanceOf(ServiceException.class);
  assertThatThrownBy(()->fx("1",basis("100","0","8",ExchangeRateDirection.SETTLEMENT_PER_ORIGINAL))).isInstanceOf(ServiceException.class);
  assertThatThrownBy(()->fx("1",basis("100","7","7",ExchangeRateDirection.SETTLEMENT_PER_ORIGINAL))).isInstanceOf(ServiceException.class);
  assertThatThrownBy(()->fx("1",new ReceivableCreateRequest.ExchangeBasis("CNY",n("100"),n("7"),n("8"),ExchangeRateDirection.SETTLEMENT_PER_ORIGINAL))).isInstanceOf(ServiceException.class);
 }
 @Test @DisplayName("非汇率业务不能夹带依据且重复生成只保留一份")void replay(){
  var basis=basis("100","7.1","7.3",ExchangeRateDirection.SETTLEMENT_PER_ORIGINAL);var line=new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,n("20"),null,null,null,null,basis);
  assertThatThrownBy(()->receivables.create(new ReceivableCreateRequest(key(),1L,1L,"CNY","SERVICE",LocalDate.now(),null,"bad",List.of(line)))).isInstanceOf(ServiceException.class);
  var request=new ReceivableCreateRequest(key(),1L,1L,"CNY","EXCHANGE_DIFFERENCE",LocalDate.now(),null,"replay",List.of(line));long doc=receivables.create(request);assertThat(receivables.create(request)).isEqualTo(doc);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_exchange_basis b JOIN oms_ar_line l ON l.id=b.line_id WHERE l.document_id=?",Integer.class,doc)).isEqualTo(1);
 }
 @Test @DisplayName("无小数结算币种按整数折算")void yen(){var basis=new ReceivableCreateRequest.ExchangeBasis("USD",n("10"),n("150.05"),n("150.15"),ExchangeRateDirection.SETTLEMENT_PER_ORIGINAL);long doc=receivables.create(new ReceivableCreateRequest(key(),1L,1L,"JPY","EXCHANGE_DIFFERENCE",LocalDate.now(),null,"yen",List.of(new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,n("1"),null,null,null,null,basis))));assertThat(queries.detail(new com.punjab.oms.ar.domain.query.finance.ReceivableDetailQuery(doc,1L)).principal().openAmount()).isEqualByComparingTo("1");}

 @Test @DisplayName("汇率依据快照失败时不遗留应收及依据")void rollback(){
  String operation=key(),reason="reject-fx-"+key(),constraint="reject_fx_"+key().replace("-","");long count=jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_exchange_basis",Long.class),documents=jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_document",Long.class);
  jdbc.execute("ALTER TABLE oms_ar_lifecycle_change ADD CONSTRAINT "+constraint+" CHECK (object_type<>'EXCHANGE_BASIS' OR JSON_UNQUOTE(JSON_EXTRACT(after_snapshot,'$.originalAmount'))<>'123.45')");
  try{
   var request=new ReceivableCreateRequest(operation,1L,1L,"CNY","EXCHANGE_DIFFERENCE",LocalDate.now(),null,reason,List.of(new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,n("24.69"),null,null,null,null,basis("123.45","7.1","7.3",ExchangeRateDirection.SETTLEMENT_PER_ORIGINAL))));
   assertThatThrownBy(()->receivables.create(request)).isInstanceOf(RuntimeException.class);
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_exchange_basis",Long.class)).isEqualTo(count);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_document",Long.class)).isEqualTo(documents);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,operation)).isZero();
  }finally{jdbc.execute("ALTER TABLE oms_ar_lifecycle_change DROP CHECK "+constraint);}
 }

 @Test @DisplayName("增加可选汇率字段后仍可重放旧版普通应收请求")void legacyReplay()throws Exception{
  String operation=key();var line=new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,n("100"),null,null,null);var request=new ReceivableCreateRequest(operation,1L,1L,"CNY","SERVICE",LocalDate.now(),null,"legacy",List.of(line));long id=receivables.create(request);
  String legacyJson=json.writeValueAsString(request).replace(",\"exchangeBasis\":null","");String legacyHash=java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(legacyJson.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  jdbc.update("UPDATE oms_ar_operation_request SET payload_hash=? WHERE request_key=? AND operation_type='CREATE'",legacyHash,operation);
  assertThat(receivables.create(request)).isEqualTo(id);
  var changed=new ReceivableCreateRequest(operation,1L,1L,"CNY","SERVICE",LocalDate.now(),null,"legacy",List.of(new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,n("101"),null,null,null)));
  assertThatThrownBy(()->receivables.create(changed)).isInstanceOf(ServiceException.class);
 }
}
