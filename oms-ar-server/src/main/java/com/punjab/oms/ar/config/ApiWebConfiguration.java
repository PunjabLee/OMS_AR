package com.punjab.oms.ar.config;
import org.springframework.context.annotation.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
/** HTTP鉴权配置，非Web测试无需访问凭据。 @author Punjab */
@Configuration(proxyBeanMethods=false) @EnableConfigurationProperties(ApiAuthProperties.class)
@ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.SERVLET)
public class ApiWebConfiguration {
 @Bean public ApiAuthenticationFilter apiAuthenticationFilter(ApiAuthProperties properties,ObjectMapper json){return new ApiAuthenticationFilter(properties,json);}
}
