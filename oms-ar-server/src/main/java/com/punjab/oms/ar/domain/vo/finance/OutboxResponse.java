package com.punjab.oms.ar.domain.vo.finance;
import java.time.LocalDateTime;
/** 运营元数据，不含租约令牌、接收端凭据或完整业务payload。 @author Punjab */
public record OutboxResponse(Long id,Long eventId,Long legalEntityId,String destination,String status,Integer attemptCount,
 LocalDateTime nextAttemptAt,LocalDateTime leaseUntil,LocalDateTime sentTime,String lastError,Long version,
 LocalDateTime createdTime,String createdBy,LocalDateTime updatedTime,String updatedBy){}
