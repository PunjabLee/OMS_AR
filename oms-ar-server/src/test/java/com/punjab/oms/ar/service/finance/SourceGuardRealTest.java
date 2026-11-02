package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.ArApplication;import com.punjab.oms.ar.base.*;import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.enums.*;
import org.springframework.boot.test.context.SpringBootTest;import org.springframework.test.context.*;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.*;import org.junit.jupiter.api.condition.EnabledIfSystemProperty;import java.math.BigDecimal;import java.time.LocalDate;import java.nio.file.*;import java.util.*;
import static org.assertj.core.api.Assertions.*;
/** scripts/test-mysql-source-guard：来源锁和取消后禁止隐式重建。 @author Punjab */
@SpringBootTest(classes=ArApplication.class,webEnvironment=SpringBootTest.WebEnvironment.NONE) @ActiveProfiles("pre")
@EnabledIfSystemProperty(named="ar.source-guard.real",matches="true") @EnabledIfSystemProperty(named="ar.source-guard.real.commit",matches="true")
class SourceGuardRealTest {
 @DynamicPropertySource static void config(DynamicPropertyRegistry p){p.add("spring.datasource.url",()->"jdbc:mysql://127.0.0.1:13306/oms_ar_test");p.add("spring.datasource.username",()->"oms_ar");p.add("spring.datasource.password",()->System.getenv("AR_TEST_PASSWORD"));}
 @Autowired ReceivableService receivables;@Autowired JdbcTemplate jdbc;
 private String key(){return UUID.randomUUID().toString();}
 private void actor(){CurrentUser.set(new CurrentUser.Identity("source-guard-test",Set.of(1L,2L),Set.of("ar:create")));}
 @BeforeEach void setup()throws Exception{assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("oms_ar_test");for(String name:List.of("V001__ar_core.sql","V002__ar_audit.sql","V011__ar_outbox.sql","V005__ar_adjustment_link.sql","V009__ar_exchange_basis.sql","V010__ar_source_guard.sql")){var path=Path.of("../scratch/dbddl/"+name);if(Files.exists(path))for(String sql:Files.readString(path).split(";"))if(!sql.isBlank())jdbc.execute(sql);}actor();}
 @AfterEach void clear(){CurrentUser.clear();}
 private ReceivableCreateRequest request(Long scope,String sourceId,String sourceLine){return new ReceivableCreateRequest(key(),1L,scope,"CNY","OUTBOUND",LocalDate.now(),null,"source guard",List.of(new ReceivableCreateRequest.Line("A",null,AmountType.PRINCIPAL,new BigDecimal("100"),null,null,new ReceivableCreateRequest.Source("WMS","OUTBOUND",sourceId,sourceId,sourceLine,key(),"price"))));}
 @Test @DisplayName("首次创建保存来源锁及初始化快照，幂等重放不重复创建")void initialize(){String source=key();var request=request(1L,source,null);long id=receivables.create(request);assertThat(receivables.create(request)).isEqualTo(id);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_source_guard WHERE source_id=?",Integer.class,source)).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_lifecycle_change c JOIN oms_ar_source_guard g ON c.object_id=g.id WHERE c.object_type='SOURCE_GUARD' AND g.source_id=?",Integer.class,source)).isEqualTo(1);}
 @Test @DisplayName("同来源改事件编号仍被blocked拦截，原请求重放只返回历史ID")void blocked(){String source=key();var initial=request(1L,source,null);long id=receivables.create(initial);jdbc.update("UPDATE oms_ar_source_guard SET blocked=TRUE WHERE source_id=?",source);var retry=request(1L,source,null);assertThatThrownBy(()->receivables.create(retry)).isInstanceOf(ServiceException.class).hasMessageContaining("作废");assertThat(receivables.create(initial)).isEqualTo(id);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,retry.requestKey())).isZero();}
 @Test @DisplayName("不同来源行主体及NULL空行号互不污染")void separation(){String source=key();receivables.create(request(1L,source,null));jdbc.update("UPDATE oms_ar_source_guard SET blocked=TRUE WHERE source_id=?",source);assertThat(receivables.create(request(1L,source,""))).isPositive();assertThat(receivables.create(request(1L,source,"1"))).isPositive();assertThat(receivables.create(request(2L,source,null))).isPositive();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_source_guard WHERE source_id=?",Integer.class,source)).isEqualTo(4);}
 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 @Test @DisplayName("相同来源不同事件并发创建只初始化一次来源身份")void concurrentInitialize()throws Exception{
  String source=key();var a=request(1L,source,null);var b=request(1L,source,null);var start=new java.util.concurrent.CountDownLatch(2);
  try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
   var one=executor.submit(()->concurrentCreate(start,a));var two=executor.submit(()->concurrentCreate(start,b));
   assertThat(one.get(10,java.util.concurrent.TimeUnit.SECONDS)).isPositive();assertThat(two.get(10,java.util.concurrent.TimeUnit.SECONDS)).isPositive();
  }
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_source_guard WHERE source_id=?",Integer.class,source)).isEqualTo(1);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_lifecycle_change c JOIN oms_ar_source_guard g ON c.object_id=g.id WHERE c.object_type='SOURCE_GUARD' AND g.source_id=?",Integer.class,source)).isEqualTo(1);
 }
 private Long concurrentCreate(java.util.concurrent.CountDownLatch start,ReceivableCreateRequest request)throws Exception{actor();try{start.countDown();if(!start.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("timeout");return receivables.create(request);}finally{CurrentUser.clear();}}
 @Test @DisplayName("来源锁被取消事务占用时创建等待并读取已提交拦截状态")void waitsForCancellation()throws Exception{
  String source=key();receivables.create(request(1L,source,null));var fresh=request(1L,source,null);var locked=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
  try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)){
   var cancellation=executor.submit(()->new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status->{jdbc.queryForObject("SELECT id FROM oms_ar_source_guard WHERE source_id=? FOR UPDATE",Long.class,source);locked.countDown();try{if(!release.await(5,java.util.concurrent.TimeUnit.SECONDS))throw new AssertionError("timeout");}catch(InterruptedException e){throw new RuntimeException(e);}jdbc.update("UPDATE oms_ar_source_guard SET blocked=TRUE WHERE source_id=?",source);return true;}));
   assertThat(locked.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();var started=new java.util.concurrent.CountDownLatch(1);
   var creation=executor.submit(()->{actor();try{started.countDown();return receivables.create(fresh);}finally{CurrentUser.clear();}});
   assertThat(started.await(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
   try{assertThatThrownBy(()->creation.get(200,java.util.concurrent.TimeUnit.MILLISECONDS)).isInstanceOf(java.util.concurrent.TimeoutException.class);}finally{release.countDown();}
   assertThat(cancellation.get(5,java.util.concurrent.TimeUnit.SECONDS)).isTrue();assertThatThrownBy(()->creation.get(5,java.util.concurrent.TimeUnit.SECONDS)).hasCauseInstanceOf(ServiceException.class);
  }
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,fresh.requestKey())).isZero();
 }
 @Test @DisplayName("同一来源价税两输出共享锁且仅记录一次初始化")void priceTax(){String source=key();var base=request(1L,source,null);var price=base.lines().getFirst();var taxSource=new ReceivableCreateRequest.Source("WMS","OUTBOUND",source,source,null,price.source().eventId(),"tax");var tax=new ReceivableCreateRequest.Line("A",null,AmountType.OUTPUT_TAX,new BigDecimal("13"),new BigDecimal("0.13"),new BigDecimal("100"),taxSource);long id=receivables.create(new ReceivableCreateRequest(base.requestKey(),1L,1L,"CNY","OUTBOUND",LocalDate.now(),null,"price tax",List.of(price,tax)));assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_line WHERE document_id=?",Integer.class,id)).isEqualTo(2);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_source_guard WHERE source_id=?",Integer.class,source)).isEqualTo(1);}
 @Test @DisplayName("初始化流水失败不留下来源锁或应收")void rollback(){String source=key(),reason="FAIL-"+key(),constraint="reject_guard_"+key().replace("-","");var base=request(1L,source,null);jdbc.execute("ALTER TABLE oms_ar_lifecycle_event ADD CONSTRAINT "+constraint+" CHECK(reason IS NULL OR reason<>'"+reason+"')");try{var failed=new ReceivableCreateRequest(base.requestKey(),1L,1L,"CNY","OUTBOUND",LocalDate.now(),null,reason,base.lines());assertThatThrownBy(()->receivables.create(failed)).isInstanceOf(RuntimeException.class);assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_source_guard WHERE source_id=?",Integer.class,source)).isZero();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,base.requestKey())).isZero();assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_line_source WHERE source_id=?",Integer.class,source)).isZero();}finally{jdbc.execute("ALTER TABLE oms_ar_lifecycle_event DROP CHECK "+constraint);}}

}
