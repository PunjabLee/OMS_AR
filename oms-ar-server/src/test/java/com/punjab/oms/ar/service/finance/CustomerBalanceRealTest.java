package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.ArApplication;import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.enums.*;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.math.BigDecimal;import java.time.LocalDate;import java.nio.file.*;import java.util.*;
import static org.assertj.core.api.Assertions.*;
/** scripts/test-mysql-balance：正负抵销及原行关联验证。 @author Punjab */
@SpringBootTest(classes=ArApplication.class,webEnvironment=SpringBootTest.WebEnvironment.NONE) @ActiveProfiles("pre")
@EnabledIfSystemProperty(named="ar.balance.real",matches="true") @EnabledIfSystemProperty(named="ar.balance.real.commit",matches="true")
class CustomerBalanceRealTest {
 @DynamicPropertySource static void config(DynamicPropertyRegistry p){p.add("spring.datasource.url",()->"jdbc:mysql://127.0.0.1:13306/oms_ar_test");p.add("spring.datasource.username",()->"oms_ar");p.add("spring.datasource.password",()->System.getenv("AR_TEST_PASSWORD"));}
 @Autowired OffsetService offsets;@Autowired CustomerBalanceService balances;@Autowired ReceivableService receivables;@Autowired SettlementService settlements;@Autowired JdbcTemplate jdbc;
 private String key(){return UUID.randomUUID().toString();} private BigDecimal n(String v){return new BigDecimal(v);}
 private void actor(){CurrentUser.set(new CurrentUser.Identity("offset-test",Set.of(1L),Set.of("ar:create","ar:read","ar:settle","ar:reverse","ar:balance:transfer")));}
 @BeforeEach void setup()throws Exception{assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("oms_ar_test");
  for(String name:List.of("V001__ar_core.sql","V002__ar_audit.sql","V011__ar_outbox.sql","V003__ar_receipt.sql","V013__ar_cash_account.sql","V015__ar_cash_receipt_lot.sql","V004__ar_settlement.sql","V005__ar_adjustment_link.sql","V006__ar_customer_balance.sql","V018__ar_credit_cash_scope.sql","V009__ar_exchange_basis.sql","V010__ar_source_guard.sql")){var file=Path.of("../scratch/dbddl/"+name);if(Files.exists(file) && com.punjab.oms.ar.ScratchCreditSchema.shouldApply(jdbc,file))for(String sql:Files.readString(file).split(";"))if(!sql.isBlank())jdbc.execute(sql);}actor();}
 @AfterEach void clear(){CurrentUser.clear();}
 private long line(AmountType type,String amount,Long group,Long original){
  long doc=receivables.create(new ReceivableCreateRequest(key(),1L,1L,"CNY","TAX_ADJUSTMENT",LocalDate.now(),null,"test",List.of(new ReceivableCreateRequest.Line("A",group,type,n(amount),type==AmountType.OUTPUT_TAX?n("0.13"):null,type==AmountType.OUTPUT_TAX?n("100"):null,null,original))));
  return jdbc.queryForObject("SELECT id FROM oms_ar_line WHERE document_id=?",Long.class,doc);
 }
 private long group(long line){return jdbc.queryForObject("SELECT group_id FROM oms_ar_line WHERE id=?",Long.class,line);}
 private BigDecimal open(long line){return jdbc.queryForObject("SELECT open_amount FROM oms_ar_line WHERE id=?",BigDecimal.class,line);}
 private OffsetPreviewRequest request(long negative,String amount,List<Long> groups,boolean more){return new OffsetPreviewRequest(1L,negative,n(amount),groups,more,AllocationStrategy.GROUP_TAX_FIRST);}

 private long version(long line){return jdbc.queryForObject("SELECT version FROM oms_ar_line WHERE id=?",Long.class,line);}
 private BalanceTransferRequest transfer(long line,String amount){return new BalanceTransferRequest(key(),1L,line,n(amount),version(line),"transfer");}
 @Test @DisplayName("部分转入独立信用余额且幂等，撤销恢复原负数")void transferReverse(){
  BigDecimal before=accountAmount();long credit=line(AmountType.OUTPUT_TAX,"-20",null,null);var request=transfer(credit,"12");long lot=balances.transfer(request);assertThat(lot).isPositive();assertThat(balances.transfer(request)).isEqualTo(lot);
  assertThat(open(credit)).isEqualByComparingTo("-8");
  assertThat(jdbc.queryForObject("SELECT transferred_amount FROM oms_ar_line WHERE id=?",BigDecimal.class,credit)).isEqualByComparingTo("-12");
  assertThat(jdbc.queryForObject("SELECT balance_amount FROM oms_ar_customer_account WHERE id=(SELECT account_id FROM oms_ar_balance_lot WHERE id=?)",BigDecimal.class,lot)).isEqualByComparingTo(before.add(n("12")));
  var detail=balances.lotDetail(new com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery(lot,1L));assertThat(detail.sourceLineId()).isEqualTo(credit);assertThat(balances.detail(new com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery(detail.accountId(),1L)).balanceAmount()).isEqualByComparingTo(before.add(n("12")));
  var reverse=new BalanceTransferReverseRequest(key(),1L,lot,"undo");assertThat(balances.reverseTransfer(reverse)).isEqualTo(lot);assertThat(balances.reverseTransfer(reverse)).isEqualTo(lot);
  assertThat(open(credit)).isEqualByComparingTo("-20");assertThat(jdbc.queryForObject("SELECT status FROM oms_ar_balance_lot WHERE id=?",String.class,lot)).isEqualTo("REVERSED");assertThat(accountAmount()).isEqualByComparingTo(before);
 }
 @Test @DisplayName("过期版本和超额转入拒绝")void stale(){
  long credit=line(AmountType.PRINCIPAL,"-20",null,null);var first=transfer(credit,"10");balances.transfer(first);
  assertThatThrownBy(()->balances.transfer(new BalanceTransferRequest(key(),1L,credit,n("10"),first.expectedLineVersion(),"stale"))).isInstanceOf(ServiceException.class);
  assertThatThrownBy(()->balances.transfer(transfer(credit,"11"))).isInstanceOf(ServiceException.class);assertThat(open(credit)).isEqualByComparingTo("-10");
 }
 @Test @DisplayName("正数及主体越权不能转入")void invalid(){long price=line(AmountType.PRINCIPAL,"20",null,null);assertThatThrownBy(()->balances.transfer(transfer(price,"10"))).isInstanceOf(ServiceException.class);
  long credit=line(AmountType.PRINCIPAL,"-20",null,null);assertThatThrownBy(()->balances.transfer(new BalanceTransferRequest(key(),2L,credit,n("10"),0L,"scope"))).isInstanceOf(RuntimeException.class);assertThat(open(credit)).isEqualByComparingTo("-20");}

 private BigDecimal accountAmount(){return jdbc.queryForObject("SELECT COALESCE((SELECT balance_amount FROM oms_ar_customer_account WHERE customer_id=1 AND legal_entity_id=1 AND currency='CNY'),0)",BigDecimal.class);}
 @Test @DisplayName("并发不同请求转入同一负数只能扣减一次")void concurrent()throws Exception{
  long credit=line(AmountType.PRINCIPAL,"-20",null,null);BigDecimal before=accountAmount();long version=version(credit);var start=new java.util.concurrent.CountDownLatch(2);
  try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
   java.util.concurrent.Callable<Boolean> call=()->{actor();try{start.countDown();if(!start.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("timeout");try{balances.transfer(new BalanceTransferRequest(key(),1L,credit,n("20"),version,"race"));return true;}catch(ServiceException expected){return false;}}finally{CurrentUser.clear();}};
   var a=executor.submit(call);var b=executor.submit(call);assertThat(List.of(a.get(10,java.util.concurrent.TimeUnit.SECONDS),b.get(10,java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
  }
  assertThat(open(credit)).isEqualByComparingTo("0");assertThat(accountAmount()).isEqualByComparingTo(before.add(n("20")));
 }
 @Test @DisplayName("转入流水失败时账户、批次、应收一起回滚")void journalFailure(){
  long credit=line(AmountType.PRINCIPAL,"-20",null,null);BigDecimal before=accountAmount();String operation=key(),reason="FAIL-"+key(),constraint="reject_balance_"+key().replace("-","");
  jdbc.execute("ALTER TABLE oms_ar_lifecycle_event ADD CONSTRAINT "+constraint+" CHECK (reason IS NULL OR reason<>'"+reason+"')");
  try{
   assertThatThrownBy(()->balances.transfer(new BalanceTransferRequest(operation,1L,credit,n("12"),version(credit),reason))).isInstanceOf(RuntimeException.class);
   assertThat(open(credit)).isEqualByComparingTo("-20");assertThat(accountAmount()).isEqualByComparingTo(before);
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_balance_lot WHERE source_line_id=?",Integer.class,credit)).isZero();
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,operation)).isZero();
  }finally{jdbc.execute("ALTER TABLE oms_ar_lifecycle_event DROP CHECK "+constraint);}
 }
 @Test @DisplayName("抵销后剩余可分次转入，撤销转入不影响抵销")void afterOffset(){
  long price=line(AmountType.PRINCIPAL,"10",null,null),credit=line(AmountType.PRINCIPAL,"-30",null,null);var request=request(credit,"10",List.of(group(price)),false);var view=offsets.preview(request);offsets.confirm(new OffsetConfirmRequest(key(),request,view.fingerprint(),"offset first"));
  long lot=balances.transfer(transfer(credit,"12"));assertThat(open(credit)).isEqualByComparingTo("-8");balances.reverseTransfer(new BalanceTransferReverseRequest(key(),1L,lot,"undo transfer"));
  assertThat(open(credit)).isEqualByComparingTo("-20");assertThat(open(price)).isEqualByComparingTo("0");assertThat(jdbc.queryForObject("SELECT settled_amount FROM oms_ar_line WHERE id=?",BigDecimal.class,credit)).isEqualByComparingTo("-10");
 }
 @Test @DisplayName("未授予转入权限不能将负数应收转余额")void permission(){long credit=line(AmountType.PRINCIPAL,"-20",null,null);CurrentUser.set(new CurrentUser.Identity("readonly",Set.of(1L),Set.of("ar:read")));assertThatThrownBy(()->balances.transfer(transfer(credit,"10"))).isInstanceOf(RuntimeException.class);assertThat(open(credit)).isEqualByComparingTo("-20");}

 @Test @DisplayName("首次开户留初始化快照且不同币种账户隔离")void newAccounts(){
  long customer=UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;var accountIds=new ArrayList<Long>();
  for(String currency:List.of("CNY","USD")){
   long doc=receivables.create(new ReceivableCreateRequest(key(),customer,1L,currency,"SERVICE",LocalDate.now(),null,"new customer",List.of(new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,n("-20"),null,null,null))));
   long line=jdbc.queryForObject("SELECT id FROM oms_ar_line WHERE document_id=?",Long.class,doc);long lot=balances.transfer(transfer(line,"12"));
   var detail=balances.lotDetail(new com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery(lot,1L));accountIds.add(detail.accountId());
   var account=balances.detail(new com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery(detail.accountId(),1L));assertThat(account.customerId()).isEqualTo(customer);assertThat(account.currency()).isEqualTo(currency);assertThat(account.balanceAmount()).isEqualByComparingTo("12");
   assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_lifecycle_change WHERE object_type='CUSTOMER_ACCOUNT' AND object_id=? AND before_snapshot IS NULL",Integer.class,account.id())).isEqualTo(1);
   assertThatThrownBy(()->balances.detail(new com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery(account.id(),2L))).isInstanceOf(RuntimeException.class);
  }
  assertThat(accountIds.getFirst()).isNotEqualTo(accountIds.getLast());
 }
}
