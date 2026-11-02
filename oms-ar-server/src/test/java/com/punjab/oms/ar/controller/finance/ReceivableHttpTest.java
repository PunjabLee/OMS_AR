package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.config.*;
import com.punjab.oms.ar.service.finance.*;
import com.punjab.oms.ar.domain.dto.finance.ReceivableCreateRequest;
import com.punjab.oms.ar.domain.query.finance.*;
import com.punjab.oms.ar.domain.vo.finance.ReceivableResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
/** 不依赖数据库的实际HTTP路由和鉴权契约。 @author Punjab */
class ReceivableHttpTest {
 private static final String TOKEN="local-integration-test-token-0123456789";
 private static final String ROOT="/api/finance/receivables";
 private MockMvc mvc;
 private String observedActor;
 @BeforeEach void setup(){
  var properties=new ApiAuthProperties();properties.setClients(List.of(new ApiAuthProperties.Client(TOKEN,"configured-user",Set.of(1L),Set.of("ar:create","ar:read"))));
  ReceivableService service=request->{observedActor=CurrentUser.require(request.legalEntityId(),"ar:create");return 42L;};
  var queries=new QueryStub();
  mvc=MockMvcBuilders.standaloneSetup(new ReceivableController(org.mockito.Mockito.mock(ReceivableVoidService.class),service,queries)).setControllerAdvice(new ApiExceptionHandler()).addFilters(new ApiAuthenticationFilter(properties,new ObjectMapper())).build();
 }
 /** 只替代服务边界，HTTP和鉴权使用真实组件。 @author Punjab */
 static class QueryStub implements ReceivableQueryService {
  public ReceivableResponse detail(ReceivableDetailQuery q){CurrentUser.require(q.legalEntityId(),"ar:read");return null;}
  public PageResult<ReceivableResponse.Document> page(ReceivablePageQuery q){CurrentUser.require(q.getLegalEntityId(),"ar:read");return new PageResult<>(0,q.getPage(),q.getSize(),List.of());}
 }
 private String body(){return """
 {"requestKey":"http-test","customerId":1,"legalEntityId":1,"currency":"CNY","businessType":"SHIPMENT","businessDate":"2026-10-31","reason":"test","lines":[{"groupKey":"A","amountType":"PRINCIPAL","amount":100}]}
 """;}
 @AfterEach void cleanup(){CurrentUser.clear();}
 @Test @DisplayName("无凭据默认拒绝") void unauthorized() throws Exception {mvc.perform(post(ROOT+"/create").contentType("application/json").content(body())).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(401));}
 @Test @DisplayName("错误凭据拒绝且不能自报身份") void spoofed() throws Exception {mvc.perform(post(ROOT+"/create").header("Authorization","Bearer bad").header("X-User","admin").contentType("application/json").content(body())).andExpect(status().isUnauthorized());}
 @Test @DisplayName("成功创建使用配置身份并清理线程上下文") void create() throws Exception {
  mvc.perform(post(ROOT+"/create").header("Authorization","Bearer "+TOKEN).header("X-User","admin").contentType("application/json").content(body())).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0)).andExpect(jsonPath("$.data").value(42));
  assertThat(observedActor).isEqualTo("configured-user");assertThatThrownBy(()->CurrentUser.require(1L,"ar:create")).isInstanceOf(ServiceException.class);
 }
 @Test @DisplayName("分页只接受POST对象并返回分页结构") void page() throws Exception {
  mvc.perform(post(ROOT+"/page").header("Authorization","Bearer "+TOKEN).contentType("application/json").content("{\"legalEntityId\":1,\"page\":1,\"size\":20}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));
 }
 @Test @DisplayName("非法请求不能调用服务") void invalid() throws Exception {
  mvc.perform(post(ROOT+"/create").header("Authorization","Bearer "+TOKEN).contentType("application/json").content("{}")).andExpect(status().isBadRequest());assertThat(observedActor).isNull();
 }

 @Test @DisplayName("跨主体返回403，之后的请求不继承身份") void forbidden() throws Exception {
  mvc.perform(post(ROOT+"/page").header("Authorization","Bearer "+TOKEN).contentType("application/json").content("{\"legalEntityId\":2}")).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(403));
  mvc.perform(post(ROOT+"/page").contentType("application/json").content("{\"legalEntityId\":1}")).andExpect(status().isUnauthorized());
 }
 @Test @DisplayName("非法JSON返回统一错误且不执行服务") void malformed() throws Exception {
  mvc.perform(post(ROOT+"/create").header("Authorization","Bearer "+TOKEN).contentType("application/json").content("{" )).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(400));assertThat(observedActor).isNull();
 }
 @Test @DisplayName("详情仅支持GET，禁止PUT更新应收") void methods() throws Exception {
  mvc.perform(get(ROOT+"/detail").header("Authorization","Bearer "+TOKEN).param("id","1").param("legalEntityId","1")).andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
  mvc.perform(put(ROOT+"/create").header("Authorization","Bearer "+TOKEN).contentType("application/json").content(body())).andExpect(status().isMethodNotAllowed());
 }
}
