package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.ArApplication;
import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.dto.finance.ReceiptCreateRequest;
import com.punjab.oms.ar.domain.query.finance.ReceiptDetailQuery;
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
/** scripts/test-mysql-receipt，隔离测试库写入门禁。 @author Punjab */
@SpringBootTest(classes=ArApplication.class,webEnvironment=SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("pre")
@EnabledIfSystemProperty(named="ar.receipt.real",matches="true")
@EnabledIfSystemProperty(named="ar.receipt.real.commit",matches="true")
class ReceiptRealTest {
 @DynamicPropertySource static void config(DynamicPropertyRegistry p){p.add("spring.datasource.url",()->"jdbc:mysql://127.0.0.1:13306/oms_ar_test");p.add("spring.datasource.username",()->"oms_ar");p.add("spring.datasource.password",()->System.getenv("AR_TEST_PASSWORD"));}
 @Autowired ReceiptService receipts;@Autowired JdbcTemplate jdbc;
 @BeforeEach void setup()throws Exception{
  assertThat(jdbc.queryForObject("SELECT DATABASE()",String.class)).isEqualTo("oms_ar_test");
  for(String name:List.of("V001__ar_core.sql","V002__ar_audit.sql","V003__ar_receipt.sql","V005__ar_adjustment_link.sql")){
   var file=Path.of("../scratch/dbddl/"+name);if(!Files.exists(file))continue;
   for(String sql:Files.readString(file).split(";"))if(!sql.isBlank())jdbc.execute(sql);
  }
  CurrentUser.set(new CurrentUser.Identity("receipt-integration",Set.of(1L),Set.of("ar:receipt:create","ar:read")));
 }
 @AfterEach void clear(){CurrentUser.clear();}
 private ReceiptCreateRequest request(String key,String source,String amount){return new ReceiptCreateRequest(key,1L,1L,"CNY",new BigDecimal(amount),LocalDate.of(2026,10,31),"BANK",source,"NO-"+source);}
 @Test @DisplayName("登记收款生成独立可用余额和初始化流水")void register(){
  String key=UUID.randomUUID().toString();var input=request(key,key,"120");long id=receipts.create(input);assertThat(id).isPositive();
  assertThat(receipts.create(input)).isEqualTo(id);var result=receipts.detail(new ReceiptDetailQuery(id,1L));assertThat(result).isNotNull();
  assertThat(result.availableAmount()).isEqualByComparingTo("120");assertThat(result.allocatedAmount()).isEqualByComparingTo("0");
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_lifecycle_change WHERE object_type='RECEIPT' AND object_id=?",Integer.class,id)).isEqualTo(1);
 }
 @Test @DisplayName("同键异内容拒绝，来源重复不能增加资金")void duplicates(){
  String key=UUID.randomUUID().toString();receipts.create(request(key,key,"100"));
  assertThatThrownBy(()->receipts.create(request(key,key,"101"))).isInstanceOf(ServiceException.class);
  String other=UUID.randomUUID().toString();assertThatThrownBy(()->receipts.create(request(other,key,"100"))).isInstanceOf(RuntimeException.class);
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM oms_ar_operation_request WHERE request_key=?",Integer.class,other)).isZero();
 }
 @Test @DisplayName("精度越界及无收款权限拒绝")void invalid(){
  assertThatThrownBy(()->receipts.create(request(UUID.randomUUID().toString(),"bad","0.001"))).isInstanceOf(ServiceException.class);
  CurrentUser.set(new CurrentUser.Identity("reader",Set.of(1L),Set.of("ar:read")));
  assertThatThrownBy(()->receipts.create(request(UUID.randomUUID().toString(),"bad","100"))).isInstanceOf(com.punjab.oms.ar.base.AccessDeniedException.class);
 }
 @Test @DisplayName("收款查询按核算主体隔离")void scope(){
  String key=UUID.randomUUID().toString();long id=receipts.create(request(key,key,"100"));
  CurrentUser.set(new CurrentUser.Identity("other",Set.of(2L),Set.of("ar:read")));
  assertThatThrownBy(()->receipts.detail(new ReceiptDetailQuery(id,2L))).isInstanceOf(ServiceException.class);
 }
}
