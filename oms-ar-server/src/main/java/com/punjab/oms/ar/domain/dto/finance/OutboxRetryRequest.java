package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 人工重试须提供当前版本和原因。 @author Punjab */
public record OutboxRetryRequest(
    /** 调用方提供的幂等请求键 */
    @NotBlank @Size(max=128) String requestKey,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
 /** 待发送通知记录ID */
 @NotNull @Positive Long outboxId,
    /** 写入时须匹配的记录版本 */
    @NotNull @PositiveOrZero Long expectedVersion,
    /** 业务操作原因 */
    @NotBlank @Size(max=500) String reason){}
