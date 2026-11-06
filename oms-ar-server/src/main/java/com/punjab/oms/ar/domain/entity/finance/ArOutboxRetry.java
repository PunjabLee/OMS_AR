package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import lombok.Getter;import lombok.Setter;import java.time.LocalDateTime;
/** 独立的人工重试审计，业务服务不提供修改/删除。 @author Punjab */
@Getter @Setter @TableName("oms_ar_outbox_retry") public class ArOutboxRetry extends ArBaseEntity {
 /** 核算主体ID，也是权限范围 */
 private Long legalEntityId;
    /** 待发送通知记录ID */
    private Long outboxId;
    /** 幂等操作记录ID */
    private Long operationId;
    /** 业务操作原因 */
    private String reason;
    /** 变更前的状态 */
    private String beforeStatus;
    /** 重试前的尝试次数 */
    private Integer beforeAttemptCount;
 /** 重试前的下次调度时间 */
 private LocalDateTime beforeNextAttemptAt;
    /** 重试前最后一次派发错误 */
    private String beforeLastError;
    /** 变更前的版本号 */
    private Long beforeVersion;
    /** 变更后的状态 */
    private String afterStatus;
    /** 重试后的尝试次数 */
    private Integer afterAttemptCount;
 /** 重试后的下次调度时间 */
 private LocalDateTime afterNextAttemptAt;
    /** 变更后的版本号 */
    private Long afterVersion;
}
