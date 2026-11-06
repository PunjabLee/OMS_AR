package com.punjab.oms.ar.domain.entity.finance;
import java.time.*;
import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
/** lifecycle_event持久化记录。 @author Punjab */
@TableName("oms_ar_lifecycle_event")
public class ArEvent extends ArBaseEntity {
    /** 生命周期事件类型 */
    private String eventType;
    /** 业务事件发生时间 */
    private LocalDateTime occurredAt;
    /** 事件写入系统的时间 */
    private LocalDateTime recordedAt;
    /** 执行动作的可信操作人 */
    private String actor;
    /** 业务操作原因 */
    private String reason;
    /** 触发事件的业务对象类型 */
    private String triggerType;
    /** 触发事件的业务对象标识 */
    private String triggerId;
    /** 串联同一业务操作的关联标识 */
    private String correlationId;
    /** 撤销事件引用的原事件ID */
    private Long reverseOfId;
    /** 快照结构版本 */
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
