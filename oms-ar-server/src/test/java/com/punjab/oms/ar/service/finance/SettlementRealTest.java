package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.ArApplication;import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.query.finance.*;
import com.punjab.oms.ar.enums.*;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.*;
import org.springframework.beans.factory.annotation.Autowired;import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;import java.time.LocalDate;import java.nio.file.*;import java.util.*;
import static org.assertj.core.api.Assertions.*;
/** scripts/test-mysql-settlement，隔离测试库验证资金与应收原子变化。 @author Punjab */
@SpringBootTest(classes=ArApplication.class,webEnvironment=SpringBootTest.WebEnvironment.NONE) @ActiveProfiles("pre")
@EnabledIfSystemProperty(named="ar.settlement.real",matches="true") @EnabledIfSystemProperty(named="ar.settlement.real.commit",matches="true")
class SettlementRealTest {
 @DynamicPropertySource static void config(DynamicPropertyRegistry p){p.add("spring.datasource.url",()->"jdbc:mysql://127.0.0.1:13306/oms_ar_test");p.add("spring.datasource.username",()->"oms_ar");p.add("spring.datasource.password",()->System.getenv("AR_TEST_PASSWORD"));}
 @Autowired SettlementService settlements;@Autowired ReceiptService receipts;@Autowired ReceivableService receivables;@Autowired ReceivableQueryService queries;@Autowired JdbcTemplate jdbc;
 private String key(){return UUID.randomUUID().toString();} private BigDecimal n(String v){return new BigDecimal(v);}
 private void actor(){CurrentUser.set(new CurrentUser.Identity("settlement-integration",Set.of(1L),Set.of("ar:create","ar:read","ar:receipt:create","ar:settle","ar:reverse")));}
 @BeforeEach void setup()throws Exception{
  assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("oms_ar_test");
  for(String name:List.of("V001__ar_core.sql","V002__ar_audit.sql","V003__ar_receipt.sql","V004__ar_settlement.sql","V005__ar_adjustment_link.sql","V009__ar_exchange_basis.sql","V010__ar_source_guard.sql")){
   var file=Path.of("../scratch/dbddl/"+name);if(!Files.exists(file))continue;for(String sql:Files.readString(file).split(";"))if(!sql.isBlank())jdbc.execute(sql);
  }actor();
 }
 @AfterEach void clear(){CurrentUser.clear();}
 private long receipt(String amount){return receipts.create(new ReceiptCreateRequest(key(),1L,1L,"CNY",n(amount),LocalDate.now(),"TEST",key(),"NO"));}
 private long document(){return receivables.create(new ReceivableCreateRequest(key(),1L,1L,"CNY","SHIPMENT",LocalDate.now(),null,"test",List.of(
  new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,n("100"),null,null,null),
  new ReceivableCreateRequest.Line("A",null,AmountType.OUTPUT_TAX,n("13"),n("0.13"),n("100"),null))));}
 private long group(long doc){return jdbc.queryForObject("SELECT MIN(group_id) FROM oms_ar_line WHERE document_id=?",Long.class,doc);}
 private SettlementPreviewRequest preview(long receipt,long group,String amount){return new SettlementPreviewRequest(1L,receipt,n(amount),List.of(group),AllocationStrategy.GROUP_TAX_FIRST);}
 @Test @DisplayName("先税后价确认与反核销精确恢复")void confirmAndReverse(){
  long doc=document(),receipt=receipt("120");var request=preview(receipt,group(doc),"20");var view=settlements.preview(request);assertThat(view).isNotNull();
  assertThat(view.allocations().getFirst().amount()).isEqualByComparingTo("13");
  var confirm=new SettlementConfirmRequest(key(),request,view.fingerprint(),"test");long id=settlements.confirm(confirm);assertThat(id).isPositive();assertThat(settlements.confirm(confirm)).isEqualTo(id);
  var detail=queries.detail(new ReceivableDetailQuery(doc,1L));assertThat(detail.principal().openAmount()).isEqualByComparingTo("93");assertThat(detail.outputTax().openAmount()).isEqualByComparingTo("0");
  assertThat(receipts.detail(new ReceiptDetailQuery(receipt,1L)).availableAmount()).isEqualByComparingTo("100");
  var reverse=new SettlementReverseRequest(key(),1L,id,"undo");assertThat(settlements.reverse(reverse)).isEqualTo(id);assertThat(settlements.reverse(reverse)).isEqualTo(id);
  assertThat(queries.detail(new ReceivableDetailQuery(doc,1L)).principal().openAmount()).isEqualByComparingTo("100");
  assertThat(queries.detail(new ReceivableDetailQuery(doc,1L)).outputTax().openAmount()).isEqualByComparingTo("13");
  assertThat(receipts.detail(new ReceiptDetailQuery(receipt,1L)).availableAmount()).isEqualByComparingTo("120");
 }
 @Test @DisplayName("预览不占款，过期预览拒绝且不留核销单")void stale(){
  long doc=document(),receipt=receipt("120");var request=preview(receipt,group(doc),"20");var first=settlements.preview(request);assertThat(first).isNotNull();
  assertThat(receipts.detail(new ReceiptDetailQuery(receipt,1L)).allocatedAmount()).isEqualByComparingTo("0");
  settlements.confirm(new SettlementConfirmRequest(key(),request,first.fingerprint(),"first"));
  String second=key();assertThatThrownBy(()->settlements.confirm(new SettlementConfirmRequest(second,request,first.fingerprint(),"stale"))).isInstanceOf(ServiceException.class);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,second)).isZero();
 }
 @Test @DisplayName("超出应收部分保留为收款可用余额")void excess(){
  long doc=document(),receipt=receipt("120");var request=preview(receipt,group(doc),"120");var view=settlements.preview(request);assertThat(view).isNotNull();assertThat(view.unallocatedAmount()).isEqualByComparingTo("7");
  settlements.confirm(new SettlementConfirmRequest(key(),request,view.fingerprint(),"excess"));assertThat(receipts.detail(new ReceiptDetailQuery(receipt,1L)).availableAmount()).isEqualByComparingTo("7");
 }
 @Test @DisplayName("补税改变目标成员后旧预览失效")void appendedTax(){
  long doc=document(),receipt=receipt("200"),group=group(doc);var request=preview(receipt,group,"20");var view=settlements.preview(request);assertThat(view).isNotNull();
  receivables.create(new ReceivableCreateRequest(key(),1L,1L,"CNY","TAX",LocalDate.now(),null,"extra",List.of(new ReceivableCreateRequest.Line("A",group,AmountType.OUTPUT_TAX,n("1"),n("0.01"),n("100"),null))));
  assertThatThrownBy(()->settlements.confirm(new SettlementConfirmRequest(key(),request,view.fingerprint(),"stale"))).isInstanceOf(ServiceException.class);
 }
 @Test @DisplayName("资金不足及预留策略拒绝")void invalid(){
  long doc=document(),receipt=receipt("10");assertThatThrownBy(()->settlements.preview(preview(receipt,group(doc),"20"))).isInstanceOf(ServiceException.class);
  assertThatThrownBy(()->settlements.preview(new SettlementPreviewRequest(1L,receipt,n("1"),List.of(group(doc)),AllocationStrategy.DOCUMENT_TAX_FIRST))).isInstanceOf(ServiceException.class);
 }

 @Test @DisplayName("同一预览并发确认只能成功一次")void concurrent()throws Exception{
  long doc=document(),receipt=receipt("120");var request=preview(receipt,group(doc),"80");var view=settlements.preview(request);
  var start=new java.util.concurrent.CountDownLatch(2);
  try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
   java.util.concurrent.Callable<Boolean> call=()->{actor();try{start.countDown();if(!start.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("timeout");try{settlements.confirm(new SettlementConfirmRequest(key(),request,view.fingerprint(),"race"));return true;}catch(ServiceException expected){return false;}}finally{CurrentUser.clear();}};
   var a=executor.submit(call);var b=executor.submit(call);assertThat(List.of(a.get(10,java.util.concurrent.TimeUnit.SECONDS),b.get(10,java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
  }
  assertThat(receipts.detail(new ReceiptDetailQuery(receipt,1L)).availableAmount()).isEqualByComparingTo("40");
  var detail=queries.detail(new ReceivableDetailQuery(doc,1L));assertThat(detail.principal().openAmount()).isEqualByComparingTo("33");
 }
 @Test @DisplayName("流水失败回滚核销单、资金、应收及幂等占位")void journalFailure(){
  long doc=document(),receipt=receipt("120");var request=preview(receipt,group(doc),"20");var view=settlements.preview(request);String operation=key();String reason="FAIL-"+key();
  String trigger="reject_event_"+UUID.randomUUID().toString().replace("-","");
  jdbc.execute("ALTER TABLE oms_ar_lifecycle_event ADD CONSTRAINT "+trigger+" CHECK (reason IS NULL OR reason<>'"+reason+"')");
  try{
   assertThatThrownBy(()->settlements.confirm(new SettlementConfirmRequest(operation,request,view.fingerprint(),reason))).isInstanceOf(RuntimeException.class);
   assertThat(receipts.detail(new ReceiptDetailQuery(receipt,1L)).availableAmount()).isEqualByComparingTo("120");
   assertThat(queries.detail(new ReceivableDetailQuery(doc,1L)).outputTax().openAmount()).isEqualByComparingTo("13");
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_settlement WHERE receipt_id=?",Integer.class,receipt)).isZero();
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,operation)).isZero();
  }finally{jdbc.execute("ALTER TABLE oms_ar_lifecycle_event DROP CHECK "+trigger);}
 }
 @Test @DisplayName("反核销较早记录不改变后续核销分配")void reverseEarlier(){
  long doc=document(),receipt=receipt("120"),group=group(doc);var first=preview(receipt,group,"10");var view=settlements.preview(first);
  long firstId=settlements.confirm(new SettlementConfirmRequest(key(),first,view.fingerprint(),"first"));
  var second=preview(receipt,group,"20");var later=settlements.preview(second);settlements.confirm(new SettlementConfirmRequest(key(),second,later.fingerprint(),"second"));
  settlements.reverse(new SettlementReverseRequest(key(),1L,firstId,"reverse first"));
  var result=queries.detail(new ReceivableDetailQuery(doc,1L));assertThat(result.outputTax().openAmount()).isEqualByComparingTo("10");assertThat(result.principal().openAmount()).isEqualByComparingTo("83");
  assertThat(receipts.detail(new ReceiptDetailQuery(receipt,1L)).availableAmount()).isEqualByComparingTo("100");
 }
}
