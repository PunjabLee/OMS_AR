package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;
/** 用户确认的抵销选择和剩余金额保留。 @author Punjab */
public record OffsetConfirmRequest(@NotBlank @Size(max=128) String requestKey,@NotNull @Valid OffsetPreviewRequest preview,
 @NotBlank @Size(min=64,max=64) String expectedFingerprint,@NotBlank @Size(max=500) String reason){}
