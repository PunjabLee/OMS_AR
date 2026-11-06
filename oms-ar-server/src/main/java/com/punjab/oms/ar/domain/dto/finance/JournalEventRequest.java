package com.punjab.oms.ar.domain.dto.finance;
import java.util.List;
/** 业务服务提交的不可变事件。 @author Punjab */
public record JournalEventRequest(
    /** 来源单据或事件的类型 */
    String type,
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
    /** 本事件涉及的对象变化明细 */
    List<Change> changes){
 /** 同一事件内的对象前后快照。 @author Punjab */
 public record Change(
    /** 被记录或诊断的对象类型 */
    String objectType,
    /** 被记录或诊断的对象ID */
    Long objectId,
    /** 变更前的对象快照，初始化时为空 */
    String beforeSnapshot,
    /** 本次变更的字段及增量 */
    String deltaSnapshot,
    /** 变更后的对象快照 */
    String afterSnapshot,
    /** 变更前的版本号 */
    Long beforeVersion,
    /** 变更后的版本号 */
    Long afterVersion){}
}
