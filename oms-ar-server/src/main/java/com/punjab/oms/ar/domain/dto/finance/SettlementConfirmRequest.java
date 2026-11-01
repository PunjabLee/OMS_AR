package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;
/** 核销确认，指纹必须来自当前预览。 @author Punjab */
public record SettlementConfirmRequest(@NotBlank @Size(max=128) String requestKey,@NotNull @Valid SettlementPreviewRequest preview,
 @NotBlank @Size(min=64,max=64) String expectedFingerprint,@NotBlank @Size(max=500) String reason){}
