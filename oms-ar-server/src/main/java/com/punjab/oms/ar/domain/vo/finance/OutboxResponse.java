package com.punjab.oms.ar.domain.vo.finance;
import java.time.LocalDateTime;
/** 运营元数据，不含租约令牌、接收端凭据或完整业务payload。 @author Punjab */
public record OutboxResponse(
    /** 当前对象ID */
    Long id,
    /** 事件标识 */
    Long eventId,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId,
    /** 通知的逻辑投递目的地 */
    String destination,
    /** 当前业务记录的状态 */
    String status,
    /** 当前投递轮次已尝试的次数 */
    Integer attemptCount,
 /** 下次允许领取通知的时间 */
 LocalDateTime nextAttemptAt,
    /** 派发租约到期时间 */
    LocalDateTime leaseUntil,
    /** 成功投递完成时间 */
    LocalDateTime sentTime,
    /** 最近一次投递错误分类 */
    String lastError,
    /** 用于并发校验的记录版本 */
    Long version,
 /** 记录创建时间 */
 LocalDateTime createdTime,
    /** 创建记录的操作人 */
    String createdBy,
    /** 记录最后更新时间 */
    LocalDateTime updatedTime,
    /** 最近更新记录的操作人 */
    String updatedBy){}
