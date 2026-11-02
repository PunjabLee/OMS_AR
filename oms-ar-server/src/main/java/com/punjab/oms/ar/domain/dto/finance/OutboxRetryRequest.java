package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 人工重试须提供当前版本和原因。 @author Punjab */
public record OutboxRetryRequest(@NotBlank @Size(max=128) String requestKey,@NotNull @Positive Long legalEntityId,
 @NotNull @Positive Long outboxId,@NotNull @PositiveOrZero Long expectedVersion,@NotBlank @Size(max=500) String reason){}
