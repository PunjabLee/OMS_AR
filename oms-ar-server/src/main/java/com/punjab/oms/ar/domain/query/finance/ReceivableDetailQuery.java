package com.punjab.oms.ar.domain.query.finance;
import jakarta.validation.constraints.*;
/** 详情范围。 @author Punjab */
public record ReceivableDetailQuery(@NotNull @Positive Long id,@NotNull @Positive Long legalEntityId){}
