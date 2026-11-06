package com.punjab.oms.ar.domain.vo.finance;
/** 内部派发数据，不含租约令牌、接收地址或凭据。 @author Punjab */
public record OutboxDeliveryResponse(
    /** 当前对象ID */
    Long id,
    /** 事件标识 */
    Long eventId,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId,
    /** 通知的逻辑投递目的地 */
    String destination,
    /** 序列化的请求或事件内容 */
    String payload,
    /** 当前投递轮次已尝试的次数 */
    Integer attemptCount){}
