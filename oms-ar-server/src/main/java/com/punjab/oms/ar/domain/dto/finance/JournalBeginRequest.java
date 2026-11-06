package com.punjab.oms.ar.domain.dto.finance;
/** 由业务服务构造，不作为HTTP契约。 @author Punjab */
public record JournalBeginRequest(
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId,
    /** 调用此动作所需的权限 */
    String permission,
    /** 业务动作标识，用于隔离幂等请求 */
    String action,
    /** 调用方提供的幂等请求键 */
    String requestKey,
    /** 序列化的请求或事件内容 */
    String payload){}
