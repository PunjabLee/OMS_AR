package com.punjab.oms.ar.domain.query.finance;
import jakarta.validation.constraints.*;
/** 收款详情范围。 @author Punjab */
public record ReceiptDetailQuery(@NotNull @Positive Long id,@NotNull @Positive Long legalEntityId){}
