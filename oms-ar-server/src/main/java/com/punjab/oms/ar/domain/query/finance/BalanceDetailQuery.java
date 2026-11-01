package com.punjab.oms.ar.domain.query.finance;
import jakarta.validation.constraints.*;
/** 账户或批次的主体限定查询。 @author Punjab */
public record BalanceDetailQuery(@NotNull @Positive Long id,@NotNull @Positive Long legalEntityId){}
