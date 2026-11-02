package com.punjab.oms.ar.domain.query.finance;
import jakarta.validation.constraints.*;
/** 核销详情访问范围。 @author Punjab */
public record SettlementDetailQuery(@NotNull @Positive Long id,@NotNull @Positive Long legalEntityId){}
