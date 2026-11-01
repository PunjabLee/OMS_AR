package com.punjab.oms.ar.domain.query.finance;
import jakarta.validation.constraints.*;
/** 发票关联详情及主体范围。 @author Punjab */
public record InvoiceDetailQuery(@NotNull @Positive Long id,@NotNull @Positive Long legalEntityId){}
