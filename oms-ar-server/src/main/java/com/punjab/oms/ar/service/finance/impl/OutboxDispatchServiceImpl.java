package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.*;import com.punjab.oms.ar.config.OutboxProperties;import com.punjab.oms.ar.extapi.finance.FinanceNotificationClient;import com.punjab.oms.ar.domain.dto.finance.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;import lombok.extern.slf4j.Slf4j;import java.util.*;
/** 每轮每个配置主体最多领取一条；HTTP位于所有业务事务之外。 @author Punjab */
@Service @RequiredArgsConstructor @Slf4j public class OutboxDispatchServiceImpl implements OutboxDispatchService {
 private final OutboxProperties properties;private final OutboxLeaseService leases;private final FinanceNotificationClient client;
 /** 逐主体领取到期通知，在事务外发送后按租约确认结果或安排重试。 */
 @Transactional(propagation=Propagation.NOT_SUPPORTED)
 public int dispatchOnce(){
  if(!properties.isEnabled())return 0;properties.validate();int claimed=0;
  for(var entry:new TreeMap<>(properties.getReceivers()).entrySet()){
   if(Thread.currentThread().isInterrupted())break;
   try{
    String token=UUID.randomUUID().toString();var delivery=leases.claim(new OutboxClaimRequest(entry.getKey(),token,properties.getMaxAttempts(),properties.getLeaseSeconds()));if(delivery==null)continue;claimed++;
    String error=client.send(entry.getValue(),delivery,properties.getRequestTimeoutMillis());
    boolean applied=leases.finish(new OutboxFinishRequest(delivery.id(),delivery.legalEntityId(),token,error==null,error,properties.getMaxAttempts(),properties.retrySeconds(delivery.attemptCount())));
    if(!applied)log.warn("Notification result ignored after lease change: scope={}, outbox={}",entry.getKey(),delivery.id());
   }catch(RuntimeException e){log.warn("Notification worker failed; lease recovery will retry: scope={}",entry.getKey());}
  }
  return claimed;
 }
}
