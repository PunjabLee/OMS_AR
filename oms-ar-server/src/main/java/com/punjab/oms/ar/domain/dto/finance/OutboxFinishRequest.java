package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 内部租约确认，错误仅允许预定义分类，不保存远端响应或异常详情。 @author Punjab */
public record OutboxFinishRequest(
    /** 当前对象ID */
    @NotNull @Positive Long id,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
 /** 派发工作者本次领取的凭证 */
 @NotBlank @Pattern(regexp="[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}") String leaseToken,
 /** 本次通知投递是否成功 */
 boolean success,
    /** 派发失败分类 */
    @Pattern(regexp="HTTP_[0-9]{3}|TIMEOUT|IO_ERROR|INTERRUPTED|INVALID_RESPONSE") String errorCode,
 /** 每轮通知允许的最大投递次数 */
 @Min(1) @Max(100) int maxAttempts,
    /** 失败后再次调度的等待秒数 */
    @Min(1) @Max(86400) int retrySeconds){}
