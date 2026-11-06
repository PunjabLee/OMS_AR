package com.punjab.oms.ar.domain.query.finance;
import jakarta.validation.constraints.*;
/** 详情范围。 @author Punjab */
public record ReceivableDetailQuery(
    /** 当前对象ID */
    @NotNull @Positive Long id,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId){}
