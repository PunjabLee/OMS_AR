package com.punjab.oms.ar.config;
import java.util.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
/** 服务端API凭据配置。 @author Punjab */
@ConfigurationProperties(prefix="ar.auth")
public class ApiAuthProperties {
 private List<Client> clients=List.of();
 public List<Client> getClients(){return clients;} public void setClients(List<Client> values){clients=List.copyOf(values);}
 /** token只从受保护配置加载，禁止用于响应或日志。 @author Punjab */
 public record Client(String token,String username,Set<Long> legalEntities,Set<String> permissions){
  @Override public String toString(){return "Client[redacted]";}
 }
}
