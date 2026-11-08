package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.ArApplication;import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.enums.*;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;import java.time.LocalDate;import java.nio.file.*;import java.util.*;
import static org.assertj.core.api.Assertions.*;
/** scripts/test-mysql-offset：正负抵销及原行关联验证。 @author Punjab */
@SpringBootTest(classes=ArApplication.class,webEnvironment=SpringBootTest.WebEnvironment.NONE) @ActiveProfiles("pre")
@EnabledIfSystemProperty(named="ar.offset.real",matches="true") @EnabledIfSystemProperty(named="ar.offset.real.commit",matches="true")
class OffsetRealTest {
 @DynamicPropertySource static void config(DynamicPropertyRegistry p){p.add("spring.datasource.url",()->"jdbc:mysql://127.0.0.1:13306/oms_ar_test");p.add("spring.datasource.username",()->"oms_ar");p.add("spring.datasource.password",()->System.getenv("AR_TEST_PASSWORD"));}
 @Autowired OffsetService offsets;@Autowired ReceivableService receivables;@Autowired SettlementService settlements;@Autowired JdbcTemplate jdbc;
 private String key(){return UUID.randomUUID().toString();} private BigDecimal n(String v){return new BigDecimal(v);}
 private void actor(){CurrentUser.set(new CurrentUser.Identity("offset-test",Set.of(1L),Set.of("ar:create","ar:read","ar:settle","ar:reverse")));}
 @BeforeEach void setup()throws Exception{assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("oms_ar_test");
  for(String name:List.of("V020__ar_cash_cutover.sql","V001__ar_core.sql","V002__ar_audit.sql","V011__ar_outbox.sql","V003__ar_receipt.sql","V013__ar_cash_account.sql","V015__ar_cash_receipt_lot.sql","V019__ar_cash_receipt_reversal.sql","V004__ar_settlement.sql","V005__ar_adjustment_link.sql","V009__ar_exchange_basis.sql","V010__ar_source_guard.sql")){var file=Path.of("../scratch/dbddl/"+name);if(Files.exists(file))for(String sql:Files.readString(file).split(";"))if(!sql.isBlank())jdbc.execute(sql);}actor();}
 @AfterEach void clear(){CurrentUser.clear();}
 private long line(AmountType type,String amount,Long group,Long original){
  long doc=receivables.create(new ReceivableCreateRequest(key(),1L,1L,"CNY","TAX_ADJUSTMENT",LocalDate.now(),null,"test",List.of(new ReceivableCreateRequest.Line("A",group,type,n(amount),type==AmountType.OUTPUT_TAX?n("0.13"):null,type==AmountType.OUTPUT_TAX?n("100"):null,null,original))));
  return jdbc.queryForObject("SELECT id FROM oms_ar_line WHERE document_id=?",Long.class,doc);
 }
 private long group(long line){return jdbc.queryForObject("SELECT group_id FROM oms_ar_line WHERE id=?",Long.class,line);}
 private BigDecimal open(long line){return jdbc.queryForObject("SELECT open_amount FROM oms_ar_line WHERE id=?",BigDecimal.class,line);}
 private OffsetPreviewRequest request(long negative,String amount,List<Long> groups,boolean more){return new OffsetPreviewRequest(1L,negative,n(amount),groups,more,AllocationStrategy.GROUP_TAX_FIRST);}
 @Test @DisplayName("独立负税允许跨类型抵销并可精确反核销")void independent(){
  long price=line(AmountType.PRINCIPAL,"100",null,null),tax=line(AmountType.OUTPUT_TAX,"5",group(price),null),credit=line(AmountType.OUTPUT_TAX,"-20",null,null);
  var request=request(credit,"20",List.of(group(price)),false);var view=offsets.preview(request);assertThat(view).isNotNull();assertThat(view.linked()).isFalse();
  assertThat(view.allocations().getFirst().lineId()).isEqualTo(tax);assertThat(view.allocations().getFirst().amount()).isEqualByComparingTo("5");
  var confirm=new OffsetConfirmRequest(key(),request,view.fingerprint(),"offset");long id=offsets.confirm(confirm);assertThat(offsets.confirm(confirm)).isEqualTo(id);
  assertThat(open(credit)).isEqualByComparingTo("0");assertThat(open(price)).isEqualByComparingTo("85");assertThat(open(tax)).isEqualByComparingTo("0");
  var reverse=new SettlementReverseRequest(key(),1L,id,"undo");assertThat(settlements.reverse(reverse)).isEqualTo(id);assertThat(settlements.reverse(reverse)).isEqualTo(id);
  assertThat(open(credit)).isEqualByComparingTo("-20");assertThat(open(price)).isEqualByComparingTo("100");assertThat(open(tax)).isEqualByComparingTo("5");
 }
 @Test @DisplayName("有关联优先原行且只抵同类型，余款保留待核销")void linked(){
  long price=line(AmountType.PRINCIPAL,"100",null,null),tax=line(AmountType.OUTPUT_TAX,"13",group(price),null),credit=line(AmountType.OUTPUT_TAX,"-20",null,tax);
  var request=request(credit,"20",List.of(),false);var view=offsets.preview(request);assertThat(view).isNotNull();assertThat(view.linked()).isTrue();assertThat(view.originalLineId()).isEqualTo(tax);assertThat(view.usedAmount()).isEqualByComparingTo("13");assertThat(view.remainingCredit()).isEqualByComparingTo("7");
  offsets.confirm(new OffsetConfirmRequest(key(),request,view.fingerprint(),"linked"));assertThat(open(price)).isEqualByComparingTo("100");assertThat(open(credit)).isEqualByComparingTo("-7");
 }
 @Test @DisplayName("用户确认后可继续抵销其他组同类型")void continueSameType(){
  long tax=line(AmountType.OUTPUT_TAX,"13",null,null),otherPrice=line(AmountType.PRINCIPAL,"200",null,null),otherTax=line(AmountType.OUTPUT_TAX,"26",group(otherPrice),null),credit=line(AmountType.OUTPUT_TAX,"-20",null,tax);
  var request=request(credit,"20",List.of(group(otherPrice)),true);var view=offsets.preview(request);assertThat(view).isNotNull();assertThat(view.allocations()).extracting(x->x.lineId()).containsExactly(tax,otherTax);
  offsets.confirm(new OffsetConfirmRequest(key(),request,view.fingerprint(),"continue"));assertThat(open(otherTax)).isEqualByComparingTo("19");assertThat(open(otherPrice)).isEqualByComparingTo("200");
 }
 @Test @DisplayName("不允许原行跨金额类型关联")void invalidOriginal(){long price=line(AmountType.PRINCIPAL,"100",null,null);assertThatThrownBy(()->line(AmountType.OUTPUT_TAX,"-10",null,price)).isInstanceOf(ServiceException.class);}
 @Test @DisplayName("重复使用负数余额的过期预览拒绝")void stale(){long price=line(AmountType.PRINCIPAL,"100",null,null),credit=line(AmountType.PRINCIPAL,"-20",null,null);var request=request(credit,"20",List.of(group(price)),false);var view=offsets.preview(request);assertThat(view).isNotNull();offsets.confirm(new OffsetConfirmRequest(key(),request,view.fingerprint(),"first"));assertThatThrownBy(()->offsets.confirm(new OffsetConfirmRequest(key(),request,view.fingerprint(),"again"))).isInstanceOf(ServiceException.class);assertThat(open(price)).isEqualByComparingTo("80");}

 @Test @DisplayName("组关联即使未指定原行也禁止跨类型")void groupLinked(){
  long price=line(AmountType.PRINCIPAL,"100",null,null),tax=line(AmountType.OUTPUT_TAX,"3",group(price),null),credit=line(AmountType.OUTPUT_TAX,"-10",group(price),null);
  var request=request(credit,"10",List.of(),false);var view=offsets.preview(request);
  assertThat(view.linked()).isTrue();assertThat(view.allocations()).extracting(x->x.lineId()).containsExactly(tax);
  offsets.confirm(new OffsetConfirmRequest(key(),request,view.fingerprint(),"group linked"));assertThat(open(price)).isEqualByComparingTo("100");assertThat(open(credit)).isEqualByComparingTo("-7");
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_adjustment_link WHERE line_id=?",Integer.class,credit)).isEqualTo(1);
 }
 @Test @DisplayName("未确认继续查找时不接受额外目标组")void explicitContinuation(){
  long tax=line(AmountType.OUTPUT_TAX,"1",null,null),other=line(AmountType.OUTPUT_TAX,"10",null,null),credit=line(AmountType.OUTPUT_TAX,"-5",null,tax);
  assertThatThrownBy(()->offsets.preview(request(credit,"5",List.of(group(other)),false))).isInstanceOf(ServiceException.class);
 }
 @Test @DisplayName("不同请求并发抵销同一负数余额只能成功一次")void concurrent()throws Exception{
  long price=line(AmountType.PRINCIPAL,"100",null,null),credit=line(AmountType.PRINCIPAL,"-20",null,null);var request=request(credit,"20",List.of(group(price)),false);var view=offsets.preview(request);
  var start=new java.util.concurrent.CountDownLatch(2);
  try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
   java.util.concurrent.Callable<Boolean> call=()->{actor();try{start.countDown();if(!start.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("timeout");try{offsets.confirm(new OffsetConfirmRequest(key(),request,view.fingerprint(),"race"));return true;}catch(ServiceException expected){return false;}}finally{CurrentUser.clear();}};
   var a=executor.submit(call);var b=executor.submit(call);assertThat(List.of(a.get(10,java.util.concurrent.TimeUnit.SECONDS),b.get(10,java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
  }
  assertThat(open(price)).isEqualByComparingTo("80");assertThat(open(credit)).isEqualByComparingTo("0");
 }
 @Test @DisplayName("抵销流水失败时双方金额和核销分配一起回滚")void journalFailure(){
  long price=line(AmountType.PRINCIPAL,"100",null,null),credit=line(AmountType.PRINCIPAL,"-20",null,null);var request=request(credit,"20",List.of(group(price)),false);var view=offsets.preview(request);String operation=key(),reason="FAIL-"+key(),constraint="reject_offset_"+key().replace("-","");
  jdbc.execute("ALTER TABLE oms_ar_lifecycle_event ADD CONSTRAINT "+constraint+" CHECK (reason IS NULL OR reason<>'"+reason+"')");
  try{
   assertThatThrownBy(()->offsets.confirm(new OffsetConfirmRequest(operation,request,view.fingerprint(),reason))).isInstanceOf(RuntimeException.class);
   assertThat(open(price)).isEqualByComparingTo("100");assertThat(open(credit)).isEqualByComparingTo("-20");
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_settlement_allocation WHERE negative_line_id=?",Integer.class,credit)).isZero();
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,operation)).isZero();
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_settlement WHERE reason=?",Integer.class,reason)).isZero();
  }finally{jdbc.execute("ALTER TABLE oms_ar_lifecycle_event DROP CHECK "+constraint);}
 }
 @Test @DisplayName("撤销早期抵销保留后来分配并记录逆向事件")void reverseEarlier(){
  long price=line(AmountType.PRINCIPAL,"100",null,null),credit=line(AmountType.PRINCIPAL,"-40",null,null);
  var first=request(credit,"10",List.of(group(price)),false);var view=offsets.preview(first);long id=offsets.confirm(new OffsetConfirmRequest(key(),first,view.fingerprint(),"first"));
  var later=request(credit,"20",List.of(group(price)),false);var second=offsets.preview(later);offsets.confirm(new OffsetConfirmRequest(key(),later,second.fingerprint(),"later"));
  settlements.reverse(new SettlementReverseRequest(key(),1L,id,"reverse earlier"));assertThat(open(price)).isEqualByComparingTo("80");assertThat(open(credit)).isEqualByComparingTo("-20");
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_lifecycle_event WHERE trigger_type='SETTLEMENT' AND trigger_id=? AND reverse_of_id IS NOT NULL",Integer.class,Long.toString(id))).isEqualTo(1);
 }
}
