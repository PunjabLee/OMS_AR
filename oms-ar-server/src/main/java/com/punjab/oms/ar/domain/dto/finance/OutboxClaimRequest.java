package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 仅内部派发器创建的租约请求，禁止绑定HTTP接口。 @author Punjab */
public record OutboxClaimRequest(
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
 /** 派发工作者本次领取的凭证 */
 @NotBlank @Pattern(regexp="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") String leaseToken,
 /** 每轮通知允许的最大投递次数 */
 @Min(1) @Max(100) int maxAttempts,
    /** 本次领取的租约时长，单位秒 */
    @Min(1) @Max(3600) int leaseSeconds){}
