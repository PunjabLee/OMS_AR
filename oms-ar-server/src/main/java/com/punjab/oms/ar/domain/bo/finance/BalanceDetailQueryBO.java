package com.punjab.oms.ar.domain.bo.finance;
/** 余额数据访问范围。 @author Punjab */
public record BalanceDetailQueryBO(
    /** 当前对象ID */
    Long id,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId){}
