package com.punjab.oms.ar.domain.query.finance;
import jakarta.validation.constraints.*;
/** 发票关联详情及主体范围。 @author Punjab */
public record InvoiceDetailQuery(
    /** 当前对象ID */
    @NotNull @Positive Long id,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId){}
