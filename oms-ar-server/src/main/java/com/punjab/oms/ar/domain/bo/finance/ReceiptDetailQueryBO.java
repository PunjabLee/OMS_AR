package com.punjab.oms.ar.domain.bo.finance;
/** 收款内部范围。 @author Punjab */
public record ReceiptDetailQueryBO(
    /** 当前对象ID */
    Long id,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId){}
