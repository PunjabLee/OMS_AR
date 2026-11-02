package com.punjab.oms.ar.domain.query.finance;
import jakarta.validation.constraints.*;
/** 主体内通知详情。 @author Punjab */
public record OutboxDetailQuery(@NotNull @Positive Long id,@NotNull @Positive Long legalEntityId){}
