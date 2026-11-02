package com.punjab.oms.ar.config;
import org.springframework.stereotype.Component;import org.springframework.boot.context.properties.ConfigurationProperties;import lombok.Getter;import lombok.Setter;import java.net.URI;import java.util.Map;
/** 服务端固定接收端；凭据只用于发送，不写入数据库或日志。 @author Punjab */
@Component @ConfigurationProperties(prefix="ar.outbox") @Getter @Setter
public class OutboxProperties {
 private boolean enabled;private boolean schedulingEnabled;private boolean allowLoopbackHttp;
 private int requestTimeoutMillis=3000;private int leaseSeconds=30;private int maxAttempts=8;
 private int retryBaseSeconds=5;private int retryMaxSeconds=3600;private int schedulingDelayMillis=5000;
 private Map<Long,Receiver> receivers=Map.of();
 @jakarta.annotation.PostConstruct public void validate(){
  if(requestTimeoutMillis<100 || requestTimeoutMillis>30000 || leaseSeconds<1 || leaseSeconds>3600 || leaseSeconds*1000L<requestTimeoutMillis+5000L || maxAttempts<1 || maxAttempts>100 || retryBaseSeconds<1 || retryMaxSeconds<retryBaseSeconds || retryMaxSeconds>86400 || schedulingDelayMillis<100 || schedulingDelayMillis>600000 || receivers==null || receivers.size()>100)throw new IllegalArgumentException("通知配置范围无效");
  for(var entry:receivers.entrySet()){
   if(entry.getKey()==null || entry.getKey()<=0 || entry.getValue()==null)throw new IllegalArgumentException("通知接收主体无效");
   var receiver=entry.getValue();var url=receiver.url();
   if(url==null || url.getHost()==null || url.getUserInfo()!=null || url.getFragment()!=null || url.getQuery()!=null || url.getPort()==0 || url.getPort()>65535)throw new IllegalArgumentException("通知接收地址无效");
   boolean loopback=java.util.Set.of("localhost","127.0.0.1","[::1]","::1").contains(url.getHost().toLowerCase(java.util.Locale.ROOT));
   if(!"https".equalsIgnoreCase(url.getScheme()) && !(allowLoopbackHttp && loopback && "http".equalsIgnoreCase(url.getScheme())))throw new IllegalArgumentException("通知接收地址须使用HTTPS，本机测试需显式允许HTTP");
   String token=receiver.bearerToken();if(token!=null && (token.length()>4096 || token.chars().anyMatch(c->c<33 || c>126)))throw new IllegalArgumentException("通知凭据格式无效");
  }
 }
 public int retrySeconds(int attempt){long value=retryBaseSeconds;for(int i=1;i<attempt && value<retryMaxSeconds;i++)value=Math.min((long)retryMaxSeconds,value*2);return (int)value;}
 /** 禁止默认toString泄露地址或凭据。 @author Punjab */
 public record Receiver(URI url,String bearerToken){@Override public String toString(){return "Receiver[redacted]";}}
}
