package com.punjab.oms.ar.domain.vo.finance;
import java.time.LocalDateTime;
/** 重试前后运行状态及操作人，不含租约或业务payload。 @author Punjab */
public record OutboxRetryResponse(Long id,Long legalEntityId,Long outboxId,String reason,String beforeStatus,Integer beforeAttemptCount,
 LocalDateTime beforeNextAttemptAt,String beforeLastError,Long beforeVersion,String afterStatus,Integer afterAttemptCount,
 LocalDateTime afterNextAttemptAt,Long afterVersion,String createdBy,LocalDateTime createdTime){}
