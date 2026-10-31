package com.punjab.oms.ar.config;
import com.punjab.oms.ar.base.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
/** 鉴权默认关闭与上下文生命周期。 @author Punjab */
class ApiAuthenticationFilterTest {
 @AfterEach void clear(){CurrentUser.clear();}
 @Test @DisplayName("未配置凭据时拒绝请求并清理旧上下文") void emptyConfiguration() throws Exception {
  CurrentUser.set(new CurrentUser.Identity("old",Set.of(1L),Set.of("ar:read")));
  var response=new MockHttpServletResponse();
  new ApiAuthenticationFilter(new ApiAuthProperties(),new ObjectMapper()).doFilter(new MockHttpServletRequest(),response,(req,res)->{throw new AssertionError("must not invoke");});
  assertThat(response.getStatus()).isEqualTo(401);assertThatThrownBy(()->CurrentUser.require(1L,"ar:read")).isInstanceOf(ServiceException.class);
 }
 @Test @DisplayName("重复或短凭据启动即拒绝") void invalidConfiguration(){
  var properties=new ApiAuthProperties();var shortKey=new ApiAuthProperties.Client("short","u",Set.of(1L),Set.of("ar:read"));
  properties.setClients(List.of(shortKey));assertThatThrownBy(()->new ApiAuthenticationFilter(properties,new ObjectMapper())).isInstanceOf(IllegalArgumentException.class);
  var key=new ApiAuthProperties.Client("a".repeat(40),"u",Set.of(1L),Set.of("ar:read"));properties.setClients(List.of(key,key));
  assertThatThrownBy(()->new ApiAuthenticationFilter(properties,new ObjectMapper())).isInstanceOf(IllegalArgumentException.class);
 }
 @Test @DisplayName("下游异常也清除身份") void exceptionCleanup(){
  var properties=new ApiAuthProperties();properties.setClients(List.of(new ApiAuthProperties.Client("a".repeat(40),"u",Set.of(1L),Set.of("ar:read"))));
  var request=new MockHttpServletRequest();request.addHeader("Authorization","Bearer "+"a".repeat(40));
  assertThatThrownBy(()->new ApiAuthenticationFilter(properties,new ObjectMapper()).doFilter(request,new MockHttpServletResponse(),(req,res)->{
   assertThat(CurrentUser.require(1L,"ar:read")).isEqualTo("u");throw new jakarta.servlet.ServletException("test failure");
  })).isInstanceOf(jakarta.servlet.ServletException.class);
  assertThatThrownBy(()->CurrentUser.require(1L,"ar:read")).isInstanceOf(ServiceException.class);
 }
}
