package com.punjab.oms.ar.domain.vo.finance;
/** 内部派发数据，不含租约令牌、接收地址或凭据。 @author Punjab */
public record OutboxDeliveryResponse(Long id,Long eventId,Long legalEntityId,String destination,String payload,Integer attemptCount){}
