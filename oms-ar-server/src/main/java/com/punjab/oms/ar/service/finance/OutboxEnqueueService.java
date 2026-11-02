package com.punjab.oms.ar.service.finance;
/** 仅内部业务服务调用，必须加入已有事务，主体来自已授权业务上下文。 @author Punjab */
public interface OutboxEnqueueService {void enqueue(Long eventId,Long legalEntityId,String actor);}
