package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 撤销未被使用的转入批次。 @author Punjab */
public record BalanceTransferReverseRequest(@NotBlank @Size(max=128) String requestKey,@NotNull @Positive Long legalEntityId,
 @NotNull @Positive Long lotId,@NotBlank @Size(max=500) String reason){}
