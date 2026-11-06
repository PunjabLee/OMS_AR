package com.punjab.oms.ar.domain.query.finance;
import jakarta.validation.constraints.*;
/** 核销详情访问范围。 @author Punjab */
public record SettlementDetailQuery(
    /** 当前对象ID */
    @NotNull @Positive Long id,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId){}
