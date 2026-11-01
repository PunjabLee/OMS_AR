package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 整单反核销。 @author Punjab */
public record SettlementReverseRequest(@NotBlank @Size(max=128) String requestKey,@NotNull @Positive Long legalEntityId,
 @NotNull @Positive Long settlementId,@NotBlank @Size(max=500) String reason){}
