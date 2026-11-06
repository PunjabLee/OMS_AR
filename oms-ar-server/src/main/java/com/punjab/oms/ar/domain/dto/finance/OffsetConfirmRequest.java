package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;
/** 用户确认的抵销选择和剩余金额保留。 @author Punjab */
public record OffsetConfirmRequest(
    /** 调用方提供的幂等请求键 */
    @NotBlank @Size(max=128) String requestKey,
    /** 用户确认的原预览请求 */
    @NotNull @Valid OffsetPreviewRequest preview,
 /** 确认时须匹配的预览指纹 */
 @NotBlank @Size(min=64,max=64) String expectedFingerprint,
    /** 业务操作原因 */
    @NotBlank @Size(max=500) String reason){}
