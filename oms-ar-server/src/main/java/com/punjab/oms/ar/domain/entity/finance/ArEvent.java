package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** lifecycle_event持久化记录。 @author Punjab */
@TableName("oms_ar_lifecycle_event")
public class ArEvent extends ArBaseEntity {
    private String eventType;
    private LocalDateTime occurredAt;
    private LocalDateTime recordedAt;
    private String actor;
    private String reason;
    private String triggerType;
    private String triggerId;
    private String correlationId;
    private Long reverseOfId;
    private Integer schemaVersion;
    public String getEventType(){return eventType;}
    public void setEventType(String value){this.eventType=value;}
    public LocalDateTime getOccurredAt(){return occurredAt;}
    public void setOccurredAt(LocalDateTime value){this.occurredAt=value;}
    public LocalDateTime getRecordedAt(){return recordedAt;}
    public void setRecordedAt(LocalDateTime value){this.recordedAt=value;}
    public String getActor(){return actor;}
    public void setActor(String value){this.actor=value;}
    public String getReason(){return reason;}
    public void setReason(String value){this.reason=value;}
    public String getTriggerType(){return triggerType;}
    public void setTriggerType(String value){this.triggerType=value;}
    public String getTriggerId(){return triggerId;}
    public void setTriggerId(String value){this.triggerId=value;}
    public String getCorrelationId(){return correlationId;}
    public void setCorrelationId(String value){this.correlationId=value;}
    public Long getReverseOfId(){return reverseOfId;}
    public void setReverseOfId(Long value){this.reverseOfId=value;}
    public Integer getSchemaVersion(){return schemaVersion;}
    public void setSchemaVersion(Integer value){this.schemaVersion=value;}
}
