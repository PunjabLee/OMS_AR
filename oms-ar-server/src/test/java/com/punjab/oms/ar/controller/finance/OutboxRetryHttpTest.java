package com.punjab.oms.ar.controller.finance;
import com.punjab.oms.ar.base.*;import com.punjab.oms.ar.config.*;import com.punjab.oms.ar.service.finance.OutboxRetryService;import com.punjab.oms.ar.domain.dto.finance.OutboxRetryRequest;import com.punjab.oms.ar.domain.query.finance.OutboxRetryPageQuery;import com.punjab.oms.ar.domain.vo.finance.OutboxRetryResponse;import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;import org.springframework.test.web.servlet.*;import org.springframework.test.web.servlet.setup.MockMvcBuilders;import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;import static org.assertj.core.api.Assertions.*;
/** 人工重试HTTP写权限、参数与身份隔离。 @author Punjab */
class OutboxRetryHttpTest {
 static final String WRITE="notification-write-test-token-0123456789",READ="notification-read-test-token-0123456789",ROOT="/api/finance/notifications";
 MockMvc mvc;String actor;
 @BeforeEach void setup(){var props=new ApiAuthProperties();props.setClients(List.of(new ApiAuthProperties.Client(WRITE,"operator",Set.of(1L),Set.of("ar:outbox:retry")),new ApiAuthProperties.Client(READ,"reader",Set.of(1L),Set.of("ar:outbox:read"))));
  var service=new OutboxRetryService(){public Long retry(OutboxRetryRequest r){actor=CurrentUser.require(r.legalEntityId(),"ar:outbox:retry");return 12L;}public PageResult<OutboxRetryResponse> history(OutboxRetryPageQuery q){CurrentUser.require(q.getLegalEntityId(),"ar:outbox:read");return new PageResult<>(0,q.getPage(),q.getSize(),List.of());}};
  mvc=MockMvcBuilders.standaloneSetup(new OutboxRetryController(service)).setControllerAdvice(new ApiExceptionHandler()).addFilters(new ApiAuthenticationFilter(props,new ObjectMapper())).build();
 }
 @AfterEach void clear(){CurrentUser.clear();}
 String body(){return "{\"requestKey\":\"retry\",\"legalEntityId\":1,\"outboxId\":42,\"expectedVersion\":0,\"reason\":\"receiver recovered\"}";}
 @Test @DisplayName("写权限独立，重试返回审计ID并使用服务端身份")void retry()throws Exception{mvc.perform(post(ROOT+"/retry").header("Authorization","Bearer "+READ).contentType("application/json").content(body())).andExpect(status().isForbidden());mvc.perform(post(ROOT+"/retry").header("Authorization","Bearer "+WRITE).header("X-User","admin").contentType("application/json").content(body())).andExpect(status().isOk()).andExpect(jsonPath("$.data").value(12));assertThat(actor).isEqualTo("operator");assertThatThrownBy(()->CurrentUser.require(1L,"ar:outbox:retry")).isInstanceOf(ServiceException.class);}
 @Test @DisplayName("无凭据与缺失必填字段拒绝")void invalid()throws Exception{mvc.perform(post(ROOT+"/retry").contentType("application/json").content(body())).andExpect(status().isUnauthorized());mvc.perform(post(ROOT+"/retry").header("Authorization","Bearer "+WRITE).contentType("application/json").content("{\"legalEntityId\":1,\"outboxId\":42,\"reason\":\" \"}")).andExpect(status().isBadRequest());assertThat(actor).isNull();}
 @Test @DisplayName("历史分页使用读权限及POST对象")void history()throws Exception{mvc.perform(post(ROOT+"/retries/page").header("Authorization","Bearer "+READ).contentType("application/json").content("{\"legalEntityId\":1,\"outboxId\":42}")).andExpect(status().isOk()).andExpect(jsonPath("$.data.total").value(0));mvc.perform(post(ROOT+"/retries/page").header("Authorization","Bearer "+WRITE).contentType("application/json").content("{\"legalEntityId\":1,\"outboxId\":42}")).andExpect(status().isForbidden());}
}
