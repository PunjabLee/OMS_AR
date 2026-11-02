package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 内部租约确认，错误仅允许预定义分类，不保存远端响应或异常详情。 @author Punjab */
public record OutboxFinishRequest(@NotNull @Positive Long id,@NotNull @Positive Long legalEntityId,
 @NotBlank @Pattern(regexp="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") String leaseToken,
 boolean success,@Pattern(regexp="HTTP_[0-9]{3}|TIMEOUT|IO_ERROR|INTERRUPTED|INVALID_RESPONSE") String errorCode,
 @Min(1) @Max(100) int maxAttempts,@Min(1) @Max(86400) int retrySeconds){}
