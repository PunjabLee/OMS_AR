package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.base.*;import com.punjab.oms.ar.config.*;import com.punjab.oms.ar.service.finance.OutboxQueryService;import com.punjab.oms.ar.domain.query.finance.*;import com.punjab.oms.ar.domain.vo.finance.*;import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;import org.springframework.test.web.servlet.*;import org.springframework.test.web.servlet.setup.MockMvcBuilders;import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.assertj.core.api.Assertions.*;
/** 实际路由和鉴权校验，服务行为由实库测试验证。 @author Punjab */
class OutboxHttpTest {
 private static final String TOKEN="notification-test-token-01234567890123";private static final String ROOT="/api/finance/notifications";
 MockMvc mvc;Long observedId;String observedActor;
 @BeforeEach void setup(){var props=new ApiAuthProperties();props.setClients(List.of(new ApiAuthProperties.Client(TOKEN,"operator",Set.of(1L),Set.of("ar:outbox:read"))));
  OutboxQueryService service=new OutboxQueryService(){
   public PageResult<OutboxResponse> page(OutboxPageQuery q){observedActor=CurrentUser.require(q.getLegalEntityId(),"ar:outbox:read");return new PageResult<>(0,q.getPage(),q.getSize(),List.of());}
   public OutboxDetailResponse detail(OutboxDetailQuery q){observedActor=CurrentUser.require(q.legalEntityId(),"ar:outbox:read");observedId=q.id();return null;}
  };
  mvc=MockMvcBuilders.standaloneSetup(new OutboxController(service)).setControllerAdvice(new ApiExceptionHandler()).addFilters(new ApiAuthenticationFilter(props,new ObjectMapper())).build();
 }
 @AfterEach void clear(){CurrentUser.clear();}
 @Test @DisplayName("通知接口缺少凭据拒绝访问")void unauthenticated()throws Exception{mvc.perform(post(ROOT+"/page").contentType("application/json").content("{\"legalEntityId\":1}")).andExpect(status().isUnauthorized());mvc.perform(get(ROOT+"/detail?id=1&legalEntityId=1")).andExpect(status().isUnauthorized());}
 @Test @DisplayName("分页POST和详情GET绑定主体，使用服务端身份")void routes()throws Exception{mvc.perform(post(ROOT+"/page").header("Authorization","Bearer "+TOKEN).header("X-User","admin").contentType("application/json").content("{\"legalEntityId\":1,\"status\":\"DEAD\",\"page\":2,\"size\":10}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.page").value(2));mvc.perform(get(ROOT+"/detail?id=42&legalEntityId=1").header("Authorization","Bearer "+TOKEN)).andExpect(status().isOk());assertThat(observedActor).isEqualTo("operator");assertThat(observedId).isEqualTo(42L);assertThatThrownBy(()->CurrentUser.require(1L,"ar:outbox:read")).isInstanceOf(ServiceException.class);}
 @Test @DisplayName("非法筛选与缺失主体拒绝，跨主体403")void validation()throws Exception{mvc.perform(post(ROOT+"/page").header("Authorization","Bearer "+TOKEN).contentType("application/json").content("{\"legalEntityId\":1,\"status\":\"OTHER\"}")).andExpect(status().isBadRequest());mvc.perform(get(ROOT+"/detail?id=42").header("Authorization","Bearer "+TOKEN)).andExpect(status().isBadRequest());assertThat(observedActor).isNull();mvc.perform(get(ROOT+"/detail?id=42&legalEntityId=2").header("Authorization","Bearer "+TOKEN)).andExpect(status().isForbidden());}
}
