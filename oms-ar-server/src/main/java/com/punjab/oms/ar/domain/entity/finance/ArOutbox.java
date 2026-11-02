package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import lombok.Getter;import lombok.Setter;import java.time.LocalDateTime;
/** 财务通知持久队列，业务payload生成后不可改写。 @author Punjab */
@Getter @Setter @TableName("oms_ar_outbox")
public class ArOutbox extends ArBaseEntity {
 private Long eventId;private Long legalEntityId;private String destination;private String payload;
 private String status;private Integer attemptCount;private LocalDateTime nextAttemptAt;
 private String leaseToken;private LocalDateTime leaseUntil;private LocalDateTime sentTime;private String lastError;private Long version;
}
