package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 仅内部派发器创建的租约请求，禁止绑定HTTP接口。 @author Punjab */
public record OutboxClaimRequest(@NotNull @Positive Long legalEntityId,
 @NotBlank @Pattern(regexp="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") String leaseToken,
 @Min(1) @Max(100) int maxAttempts,@Min(1) @Max(3600) int leaseSeconds){}
