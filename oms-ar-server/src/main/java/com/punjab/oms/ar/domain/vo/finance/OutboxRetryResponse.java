package com.punjab.oms.ar.domain.vo.finance;
import java.time.LocalDateTime;
/** 重试前后运行状态及操作人，不含租约或业务payload。 @author Punjab */
public record OutboxRetryResponse(
    /** 当前对象ID */
    Long id,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId,
    /** 待发送通知记录ID */
    Long outboxId,
    /** 业务操作原因 */
    String reason,
    /** 变更前的状态 */
    String beforeStatus,
    /** 重试前的尝试次数 */
    Integer beforeAttemptCount,
 /** 重试前的下次调度时间 */
 LocalDateTime beforeNextAttemptAt,
    /** 重试前最后一次派发错误 */
    String beforeLastError,
    /** 变更前的版本号 */
    Long beforeVersion,
    /** 变更后的状态 */
    String afterStatus,
    /** 重试后的尝试次数 */
    Integer afterAttemptCount,
 /** 重试后的下次调度时间 */
 LocalDateTime afterNextAttemptAt,
    /** 变更后的版本号 */
    Long afterVersion,
    /** 创建记录的操作人 */
    String createdBy,
    /** 记录创建时间 */
    LocalDateTime createdTime){}
