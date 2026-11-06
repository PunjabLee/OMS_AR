package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import lombok.Getter;import lombok.Setter;import java.time.LocalDateTime;
/** 财务通知持久队列，业务payload生成后不可改写。 @author Punjab */
@Getter @Setter @TableName("oms_ar_outbox")
public class ArOutbox extends ArBaseEntity {
 /** 事件标识 */
 private Long eventId;
    /** 核算主体ID，也是权限范围 */
    private Long legalEntityId;
    /** 通知的逻辑投递目的地 */
    private String destination;
    /** 序列化的请求或事件内容 */
    private String payload;
 /** 当前业务记录的状态 */
 private String status;
    /** 当前投递轮次已尝试的次数 */
    private Integer attemptCount;
    /** 下次允许领取通知的时间 */
    private LocalDateTime nextAttemptAt;
 /** 派发工作者本次领取的凭证 */
 private String leaseToken;
    /** 派发租约到期时间 */
    private LocalDateTime leaseUntil;
    /** 成功投递完成时间 */
    private LocalDateTime sentTime;
    /** 最近一次投递错误分类 */
    private String lastError;
    /** 用于并发校验的记录版本 */
    private Long version;
}
