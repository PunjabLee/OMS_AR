package com.punjab.oms.ar.domain.query.finance;
import jakarta.validation.constraints.*;
/** 账户或批次的主体限定查询。 @author Punjab */
public record BalanceDetailQuery(
    /** 当前对象ID */
    @NotNull @Positive Long id,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId){}
