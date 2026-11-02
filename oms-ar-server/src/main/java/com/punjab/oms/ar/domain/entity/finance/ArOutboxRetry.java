package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import lombok.Getter;import lombok.Setter;import java.time.LocalDateTime;
/** 独立的人工重试审计，业务服务不提供修改/删除。 @author Punjab */
@Getter @Setter @TableName("oms_ar_outbox_retry") public class ArOutboxRetry extends ArBaseEntity {
 private Long legalEntityId;private Long outboxId;private Long operationId;private String reason;private String beforeStatus;private Integer beforeAttemptCount;
 private LocalDateTime beforeNextAttemptAt;private String beforeLastError;private Long beforeVersion;private String afterStatus;private Integer afterAttemptCount;
 private LocalDateTime afterNextAttemptAt;private Long afterVersion;
}
