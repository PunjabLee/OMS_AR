package com.punjab.oms.ar.domain.bo.finance;
/** 核销详情持久层范围。 @author Punjab */
public record SettlementDetailQueryBO(
    /** 当前对象ID */
    Long id,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId){}
