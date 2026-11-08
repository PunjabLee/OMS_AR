package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.ArApplication;import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.enums.*;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;import java.time.LocalDate;import java.nio.file.*;import java.util.*;
import static org.assertj.core.api.Assertions.*;
/** scripts/test-mysql-balance-settlement：客户信用余额抵扣及批次依赖验证。 @author Punjab */
@SpringBootTest(classes=ArApplication.class,webEnvironment=SpringBootTest.WebEnvironment.NONE) @ActiveProfiles("pre")
@EnabledIfSystemProperty(named="ar.balance-settlement.real",matches="true") @EnabledIfSystemProperty(named="ar.balance-settlement.real.commit",matches="true")
class BalanceSettlementRealTest {
 private static final long scopedCustomer=UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
 @Autowired CashAccountService cash;
 @DynamicPropertySource static void config(DynamicPropertyRegistry p){p.add("ar.cash.binding-scopes",()->"1/"+scopedCustomer+"/CNY");p.add("spring.datasource.url",()->"jdbc:mysql://127.0.0.1:13306/oms_ar_test");p.add("spring.datasource.username",()->"oms_ar");p.add("spring.datasource.password",()->System.getenv("AR_TEST_PASSWORD"));}
 @Autowired BalanceSettlementService spending;@Autowired CustomerBalanceService balances;@Autowired ReceivableService receivables;@Autowired SettlementService settlements;@Autowired JdbcTemplate jdbc;
 private long customer; private String key(){return UUID.randomUUID().toString();} private BigDecimal n(String v){return new BigDecimal(v);}
 private void actor(){CurrentUser.set(new CurrentUser.Identity("balance-spend-test",Set.of(1L),Set.of("ar:create","ar:read","ar:settle","ar:reverse","ar:balance:transfer","ar:cash:open")));}
 @BeforeEach void setup()throws Exception{assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("oms_ar_test");
  for(String name:List.of("V001__ar_core.sql","V002__ar_audit.sql","V011__ar_outbox.sql","V003__ar_receipt.sql","V013__ar_cash_account.sql","V014__ar_cash_group_binding.sql","V015__ar_cash_receipt_lot.sql","V019__ar_cash_receipt_reversal.sql","V004__ar_settlement.sql","V005__ar_adjustment_link.sql","V006__ar_customer_balance.sql","V018__ar_credit_cash_scope.sql","V007__ar_balance_settlement.sql","V009__ar_exchange_basis.sql","V010__ar_source_guard.sql")){var file=Path.of("../scratch/dbddl/"+name);if(Files.exists(file) && com.punjab.oms.ar.ScratchCreditSchema.shouldApply(jdbc,file))for(String sql:Files.readString(file).split(";"))if(!sql.isBlank())jdbc.execute(sql);}actor();customer=UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;}
 @AfterEach void clear(){CurrentUser.clear();}
 private long line(AmountType type,String amount,Long group,Long original){
  long doc=receivables.create(new ReceivableCreateRequest(key(),customer,1L,"CNY","TAX_ADJUSTMENT",LocalDate.now(),null,"test",List.of(new ReceivableCreateRequest.Line("A",group,type,n(amount),type==AmountType.OUTPUT_TAX?n("0.13"):null,type==AmountType.OUTPUT_TAX?n("100"):null,null,original))));
  return jdbc.queryForObject("SELECT id FROM oms_ar_line WHERE document_id=?",Long.class,doc);
 }
 private long scopedLine(String mode,String amount){
  var input=new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,n(amount),null,null,null,null,null,mode);
  long doc=receivables.create(new ReceivableCreateRequest(key(),customer,1L,"CNY","TAX_ADJUSTMENT",LocalDate.now(),null,"test",List.of(input)));
  return jdbc.queryForObject("SELECT id FROM oms_ar_line WHERE document_id=?",Long.class,doc);
 }
 @Test @DisplayName("同客户双户信用余额独立，跨户拒绝且冲正退回原批次") void scopedCredit(){
  customer=scopedCustomer;
  long aa=cash.open(new CashAccountOpenRequest(key(),1L,customer,"CNY","INVOICE","test"));
  long bb=cash.open(new CashAccountOpenRequest(key(),1L,customer,"CNY","NO_INVOICE","test"));
  long lotA=balances.transfer(transfer(scopedLine("INVOICE","-20"),"20"));
  long lotB=balances.transfer(transfer(scopedLine("NO_INVOICE","-9"),"9"));
  long accountA=account(lotA),accountB=account(lotB);
  assertThat(accountA).isNotEqualTo(accountB);
  assertThat(balances.lotDetail(new com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery(lotA,1L)).cashAccountId()).isEqualTo(aa);
  assertThat(balances.detail(new com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery(accountB,1L)).cashAccountId()).isEqualTo(bb);
  long a=group(scopedLine("INVOICE","10")),b=group(scopedLine("NO_INVOICE","10"));
  var cross=preview(accountA,"3",a,b);
  assertThatThrownBy(()->spending.preview(cross)).isInstanceOf(ServiceException.class).hasMessageContaining("同一现金账户");
  var same=preview(accountA,"3",a);
  long settlement=spending.confirm(new BalanceSettlementConfirmRequest(key(),same,spending.preview(same).fingerprint(),"spend"));
  assertThat(balance(accountA)).isEqualByComparingTo("17");assertThat(used(lotB)).isEqualByComparingTo("0");
  settlements.reverse(new SettlementReverseRequest(key(),1L,settlement,"undo"));
  assertThat(used(lotA)).isEqualByComparingTo("0");
  balances.reverseTransfer(new BalanceTransferReverseRequest(key(),1L,lotA,"undo"));
  assertThat(balance(accountA)).isEqualByComparingTo("0");assertThat(balance(accountB)).isEqualByComparingTo("9");
 }
 private long group(long line){return jdbc.queryForObject("SELECT group_id FROM oms_ar_line WHERE id=?",Long.class,line);}
 private BigDecimal open(long line){return jdbc.queryForObject("SELECT open_amount FROM oms_ar_line WHERE id=?",BigDecimal.class,line);}

 private long version(long line){return jdbc.queryForObject("SELECT version FROM oms_ar_line WHERE id=?",Long.class,line);}
 private BalanceTransferRequest transfer(long line,String amount){return new BalanceTransferRequest(key(),1L,line,n(amount),version(line),"transfer");}

 private long credit(String amount){long source=line(AmountType.PRINCIPAL,"-"+amount,null,null);return balances.transfer(transfer(source,amount));}
 private long account(long lot){return balances.lotDetail(new com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery(lot,1L)).accountId();}
 private BigDecimal balance(long account){return balances.detail(new com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery(account,1L)).balanceAmount();}
 private BigDecimal used(long lot){return balances.lotDetail(new com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery(lot,1L)).usedAmount();}
 private BalanceSettlementPreviewRequest preview(long account,String amount,Long... groups){return new BalanceSettlementPreviewRequest(1L,account,n(amount),List.of(groups),AllocationStrategy.GROUP_TAX_FIRST);}
 @Test @DisplayName("余额按组先税后价及FIFO批次分摊，反核销后才能撤销转入")void lifecycle(){
  long first=credit("10"),second=credit("20"),account=account(first),price=line(AmountType.PRINCIPAL,"100",null,null),tax=line(AmountType.OUTPUT_TAX,"13",group(price),null);
  var request=preview(account,"25",group(price));var view=spending.preview(request);assertThat(view).isNotNull();
  assertThat(view.allocations()).extracting(x->x.lineId()).containsExactly(tax,price);assertThat(view.lots()).extracting(x->x.lotId()).containsExactly(first,second,second);
  assertThat(balance(account)).isEqualByComparingTo("30");var confirm=new BalanceSettlementConfirmRequest(key(),request,view.fingerprint(),"spend");long id=spending.confirm(confirm);assertThat(spending.confirm(confirm)).isEqualTo(id);
  assertThat(balance(account)).isEqualByComparingTo("5");assertThat(used(first)).isEqualByComparingTo("10");assertThat(used(second)).isEqualByComparingTo("15");assertThat(open(tax)).isEqualByComparingTo("0");assertThat(open(price)).isEqualByComparingTo("88");
  assertThatThrownBy(()->balances.reverseTransfer(new BalanceTransferReverseRequest(key(),1L,first,"must fail"))).isInstanceOf(ServiceException.class);
  var reverse=new SettlementReverseRequest(key(),1L,id,"undo spend");assertThat(settlements.reverse(reverse)).isEqualTo(id);assertThat(settlements.reverse(reverse)).isEqualTo(id);
  assertThat(balance(account)).isEqualByComparingTo("30");assertThat(used(first)).isEqualByComparingTo("0");assertThat(used(second)).isEqualByComparingTo("0");assertThat(open(price)).isEqualByComparingTo("100");assertThat(open(tax)).isEqualByComparingTo("13");
  balances.reverseTransfer(new BalanceTransferReverseRequest(key(),1L,first,"undo transfer"));assertThat(balance(account)).isEqualByComparingTo("20");
 }
 @Test @DisplayName("目标不足只扣实际金额，预留策略拒绝")void excess(){long lot=credit("30"),account=account(lot),price=line(AmountType.PRINCIPAL,"7",null,null);var request=preview(account,"20",group(price));var view=spending.preview(request);assertThat(view).isNotNull();assertThat(view.unallocatedAmount()).isEqualByComparingTo("13");spending.confirm(new BalanceSettlementConfirmRequest(key(),request,view.fingerprint(),"partial"));assertThat(balance(account)).isEqualByComparingTo("23");assertThat(used(lot)).isEqualByComparingTo("7");assertThatThrownBy(()->spending.preview(new BalanceSettlementPreviewRequest(1L,account,n("1"),List.of(group(price)),AllocationStrategy.DOCUMENT_TAX_FIRST))).isInstanceOf(ServiceException.class);}
 @Test @DisplayName("转入变化使旧预览失效")void stale(){long lot=credit("30"),account=account(lot),price=line(AmountType.PRINCIPAL,"100",null,null);var request=preview(account,"20",group(price));var view=spending.preview(request);assertThat(view).isNotNull();credit("5");assertThatThrownBy(()->spending.confirm(new BalanceSettlementConfirmRequest(key(),request,view.fingerprint(),"stale"))).isInstanceOf(ServiceException.class);assertThat(balance(account)).isEqualByComparingTo("35");}

 @Test @DisplayName("并发确认同一余额预览只成功一次")void concurrent()throws Exception{
  long lot=credit("30"),account=account(lot),price=line(AmountType.PRINCIPAL,"100",null,null);var request=preview(account,"20",group(price));var view=spending.preview(request);var start=new java.util.concurrent.CountDownLatch(2);
  try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
   java.util.concurrent.Callable<Boolean> call=()->{actor();try{start.countDown();if(!start.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("timeout");try{spending.confirm(new BalanceSettlementConfirmRequest(key(),request,view.fingerprint(),"race"));return true;}catch(ServiceException expected){return false;}}finally{CurrentUser.clear();}};
   var a=executor.submit(call);var b=executor.submit(call);assertThat(List.of(a.get(10,java.util.concurrent.TimeUnit.SECONDS),b.get(10,java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
  }
  assertThat(balance(account)).isEqualByComparingTo("10");assertThat(used(lot)).isEqualByComparingTo("20");assertThat(open(price)).isEqualByComparingTo("80");
 }
 @Test @DisplayName("抵扣与撤销来源转入竞争时最多成功一项")void transferRace()throws Exception{
  long lot=credit("30"),account=account(lot),price=line(AmountType.PRINCIPAL,"100",null,null);var request=preview(account,"20",group(price));var view=spending.preview(request);var start=new java.util.concurrent.CountDownLatch(2);
  try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
   java.util.concurrent.Callable<Boolean> spend=()->race(start,()->spending.confirm(new BalanceSettlementConfirmRequest(key(),request,view.fingerprint(),"race spend")));
   java.util.concurrent.Callable<Boolean> reverse=()->race(start,()->balances.reverseTransfer(new BalanceTransferReverseRequest(key(),1L,lot,"race reverse")));
   var a=executor.submit(spend);var b=executor.submit(reverse);boolean spent=a.get(10,java.util.concurrent.TimeUnit.SECONDS),reversed=b.get(10,java.util.concurrent.TimeUnit.SECONDS);assertThat(spent).isNotEqualTo(reversed);
   assertThat(balance(account)).isEqualByComparingTo(spent?"10":"0");assertThat(open(price)).isEqualByComparingTo(spent?"80":"100");
  }
 }
 private boolean race(java.util.concurrent.CountDownLatch start,java.util.concurrent.Callable<Long> action)throws Exception{actor();try{start.countDown();if(!start.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("timeout");try{action.call();return true;}catch(ServiceException expected){return false;}}finally{CurrentUser.clear();}}
 @Test @DisplayName("核销事件失败整体回滚账户、批次、应收和分配")void journalFailure(){
  long lot=credit("30"),account=account(lot),price=line(AmountType.PRINCIPAL,"100",null,null);var request=preview(account,"20",group(price));var view=spending.preview(request);String operation=key(),reason="FAIL-"+key(),constraint="reject_spend_"+key().replace("-","");
  jdbc.execute("ALTER TABLE oms_ar_lifecycle_event ADD CONSTRAINT "+constraint+" CHECK (reason IS NULL OR reason<>'"+reason+"')");
  try{
   assertThatThrownBy(()->spending.confirm(new BalanceSettlementConfirmRequest(operation,request,view.fingerprint(),reason))).isInstanceOf(RuntimeException.class);
   assertThat(balance(account)).isEqualByComparingTo("30");assertThat(used(lot)).isEqualByComparingTo("0");assertThat(open(price)).isEqualByComparingTo("100");
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_balance_usage WHERE lot_id=?",Integer.class,lot)).isZero();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_settlement WHERE reason=?",Integer.class,reason)).isZero();
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,operation)).isZero();
  }finally{jdbc.execute("ALTER TABLE oms_ar_lifecycle_event DROP CHECK "+constraint);}
 }
 @Test @DisplayName("撤销较早核销保留后续来源批次占用")void reverseEarlier(){
  long lot=credit("30"),account=account(lot),price=line(AmountType.PRINCIPAL,"100",null,null);var request=preview(account,"5",group(price));var view=spending.preview(request);long id=spending.confirm(new BalanceSettlementConfirmRequest(key(),request,view.fingerprint(),"first"));
  var later=preview(account,"10",group(price));spending.confirm(new BalanceSettlementConfirmRequest(key(),later,spending.preview(later).fingerprint(),"later"));settlements.reverse(new SettlementReverseRequest(key(),1L,id,"undo first"));
  assertThat(balance(account)).isEqualByComparingTo("20");assertThat(used(lot)).isEqualByComparingTo("10");assertThat(open(price)).isEqualByComparingTo("90");assertThatThrownBy(()->balances.reverseTransfer(new BalanceTransferReverseRequest(key(),1L,lot,"still in use"))).isInstanceOf(ServiceException.class);
 }
 @Test @DisplayName("跨客户和跨币种应收不可由账户抵扣")void scope(){
  long lot=credit("30"),account=account(lot);long originalCustomer=customer;customer=customer==1?2:1;long other=line(AmountType.PRINCIPAL,"100",null,null);customer=originalCustomer;
  assertThatThrownBy(()->spending.preview(preview(account,"10",group(other)))).isInstanceOf(ServiceException.class);
  long usdDoc=receivables.create(new ReceivableCreateRequest(key(),customer,1L,"USD","SERVICE",LocalDate.now(),null,"scope",List.of(new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,n("100"),null,null,null))));
  long usdGroup=jdbc.queryForObject("SELECT group_id FROM oms_ar_line WHERE document_id=?",Long.class,usdDoc);assertThatThrownBy(()->spending.preview(preview(account,"10",usdGroup))).isInstanceOf(ServiceException.class);assertThat(balance(account)).isEqualByComparingTo("30");
 }
 @Test @DisplayName("应收组后补税使旧余额抵扣预览失效")void newTax(){long lot=credit("30"),account=account(lot),price=line(AmountType.PRINCIPAL,"100",null,null);var request=preview(account,"20",group(price));var view=spending.preview(request);line(AmountType.OUTPUT_TAX,"3",group(price),null);assertThatThrownBy(()->spending.confirm(new BalanceSettlementConfirmRequest(key(),request,view.fingerprint(),"stale members"))).isInstanceOf(ServiceException.class);assertThat(balance(account)).isEqualByComparingTo("30");}

 @Test @DisplayName("反核销事件失败时不提前释放来源批次")void reverseFailure(){
  long lot=credit("30"),account=account(lot),price=line(AmountType.PRINCIPAL,"100",null,null);var request=preview(account,"20",group(price));var view=spending.preview(request);long id=spending.confirm(new BalanceSettlementConfirmRequest(key(),request,view.fingerprint(),"spend"));
  String operation=key(),reason="FAIL-"+key(),constraint="reject_reverse_"+key().replace("-","");jdbc.execute("ALTER TABLE oms_ar_lifecycle_event ADD CONSTRAINT "+constraint+" CHECK (reason IS NULL OR reason<>'"+reason+"')");
  try{
   assertThatThrownBy(()->settlements.reverse(new SettlementReverseRequest(operation,1L,id,reason))).isInstanceOf(RuntimeException.class);
   assertThat(balance(account)).isEqualByComparingTo("10");assertThat(used(lot)).isEqualByComparingTo("20");assertThat(open(price)).isEqualByComparingTo("80");assertThat(jdbc.queryForObject("SELECT status FROM oms_ar_settlement WHERE id=?",String.class,id)).isEqualTo("POSTED");
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,operation)).isZero();
   assertThatThrownBy(()->balances.reverseTransfer(new BalanceTransferReverseRequest(key(),1L,lot,"still used"))).isInstanceOf(ServiceException.class);
  }finally{jdbc.execute("ALTER TABLE oms_ar_lifecycle_event DROP CHECK "+constraint);}
 }
}
