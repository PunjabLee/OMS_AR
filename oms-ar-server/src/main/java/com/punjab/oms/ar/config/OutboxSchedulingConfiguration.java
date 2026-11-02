package com.punjab.oms.ar.config;
import com.punjab.oms.ar.service.finance.OutboxDispatchService;import org.springframework.context.annotation.*;import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;import org.springframework.scheduling.annotation.*;import lombok.RequiredArgsConstructor;
/** 两个开关均显式开启后才注册调度；未配置主体不会领取。 @author Punjab */
@Configuration(proxyBeanMethods=false) @EnableScheduling
@ConditionalOnProperty(prefix="ar.outbox",name={"enabled","scheduling-enabled"},havingValue="true")
@RequiredArgsConstructor public class OutboxSchedulingConfiguration {
 private final OutboxDispatchService dispatcher;
 @Scheduled(fixedDelayString="${ar.outbox.scheduling-delay-millis:5000}") public void dispatch(){dispatcher.dispatchOnce();}
}
