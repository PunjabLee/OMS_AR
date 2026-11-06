package com.punjab.oms.ar.domain.vo.finance;
import java.time.LocalDateTime;import java.util.List;import com.fasterxml.jackson.databind.JsonNode;
/** 事件及当前单据内明细的变化，不暴露同事件其他单据快照。 @author Punjab */
public record LifecycleResponse(
    /** 当前对象ID */
    Long id,
    /** 生命周期事件类型 */
    String eventType,
    /** 业务事件发生时间 */
    LocalDateTime occurredAt,
    /** 事件写入系统的时间 */
    LocalDateTime recordedAt,
    /** 执行动作的可信操作人 */
    String actor,
    /** 业务操作原因 */
    String reason,
    /** 触发事件的业务对象类型 */
    String triggerType,
    /** 触发事件的业务对象标识 */
    String triggerId,
    /** 串联同一业务操作的关联标识 */
    String correlationId,
    /** 撤销事件引用的原事件ID */
    Long reverseOfId,
    /** 快照结构版本 */
    Integer schemaVersion,
    /** 本事件涉及的对象变化明细 */
    List<Change> changes){
 public LifecycleResponse{changes=List.copyOf(changes);}
 /** JSON前额/增量/后额和版本。 @author Punjab */
 public record Change(
    /** 当前对象ID */
    Long id,
    /** 被记录或诊断的对象类型 */
    String objectType,
    /** 被记录或诊断的对象ID */
    Long objectId,
    /** 变更前的对象快照，初始化时为空 */
    JsonNode beforeSnapshot,
    /** 本次变更的字段及增量 */
    JsonNode deltaSnapshot,
    /** 变更后的对象快照 */
    JsonNode afterSnapshot,
    /** 变更前的版本号 */
    Long beforeVersion,
    /** 变更后的版本号 */
    Long afterVersion){}
}
