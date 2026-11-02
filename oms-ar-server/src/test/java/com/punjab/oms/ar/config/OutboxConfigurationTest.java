package com.punjab.oms.ar.config;
import com.punjab.oms.ar.service.finance.OutboxDispatchService;import org.junit.jupiter.api.*;import org.springframework.boot.test.context.runner.ApplicationContextRunner;import java.net.URI;import java.util.Map;
import static org.assertj.core.api.Assertions.*;import static org.mockito.Mockito.*;
/** 不访问数据库或网络的调度和配置边界。 @author Punjab */
class OutboxConfigurationTest {
 @Test @DisplayName("调度必须两个开关都明确启用")void scheduling(){
  var runner=new ApplicationContextRunner().withUserConfiguration(OutboxSchedulingConfiguration.class).withBean(OutboxDispatchService.class,()->mock(OutboxDispatchService.class));
  runner.run(c->assertThat(c).doesNotHaveBean(OutboxSchedulingConfiguration.class));
  runner.withPropertyValues("ar.outbox.enabled=true").run(c->assertThat(c).doesNotHaveBean(OutboxSchedulingConfiguration.class));
  runner.withPropertyValues("ar.outbox.enabled=true","ar.outbox.scheduling-enabled=true").run(c->assertThat(c).hasSingleBean(OutboxSchedulingConfiguration.class));
 }
 @Test @DisplayName("地址凭据和租约范围校验，不泄露配置值")void receivers(){
  var p=new OutboxProperties();p.validate();
  p.setReceivers(Map.of(1L,new OutboxProperties.Receiver(URI.create("http://127.0.0.1:1234/events"),"token")));assertThatThrownBy(p::validate).isInstanceOf(IllegalArgumentException.class);
  p.setAllowLoopbackHttp(true);p.validate();
  for(String url:new String[]{"http://example.com/events","https://example.com/events?secret=value","https://example.com/events#secret","https://user:secret@example.com/events","file:///tmp/events"}){p.setReceivers(Map.of(1L,new OutboxProperties.Receiver(URI.create(url),"token")));assertThatThrownBy(p::validate).isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("secret");}
  p.setReceivers(Map.of(1L,new OutboxProperties.Receiver(URI.create("https://example.com/events"),"bad\r\nheader")));assertThatThrownBy(p::validate).isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("bad");
  p.setReceivers(Map.of(1L,new OutboxProperties.Receiver(URI.create("https://example.com/events"),"secret")));p.validate();assertThat(p.getReceivers().toString()).doesNotContain("secret","example.com");
 }
 @Test @DisplayName("指数退避有界，不因重试次数大而溢出")void backoff(){var p=new OutboxProperties();assertThat(p.retrySeconds(1)).isEqualTo(5);assertThat(p.retrySeconds(2)).isEqualTo(10);assertThat(p.retrySeconds(3)).isEqualTo(20);assertThat(p.retrySeconds(100)).isEqualTo(3600);p.setRetryBaseSeconds(2);p.setRetryMaxSeconds(3);assertThat(p.retrySeconds(2)).isEqualTo(3);assertThat(p.retrySeconds(Integer.MAX_VALUE)).isEqualTo(3);}
}
