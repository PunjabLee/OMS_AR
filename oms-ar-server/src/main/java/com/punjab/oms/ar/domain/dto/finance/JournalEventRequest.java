package com.punjab.oms.ar.domain.dto.finance;
import java.util.List;
/** 业务服务提交的不可变事件。 @author Punjab */
public record JournalEventRequest(String type,String actor,String reason,String triggerType,String triggerId,String correlationId,Long reverseOfId,List<Change> changes){
 /** 同一事件内的对象前后快照。 @author Punjab */
 public record Change(String objectType,Long objectId,String beforeSnapshot,String deltaSnapshot,String afterSnapshot,Long beforeVersion,Long afterVersion){}
}
