package com.punjab.oms.ar.domain.vo.finance;
import java.time.LocalDateTime;import java.util.List;import com.fasterxml.jackson.databind.JsonNode;
/** 事件及当前单据内明细的变化，不暴露同事件其他单据快照。 @author Punjab */
public record LifecycleResponse(Long id,String eventType,LocalDateTime occurredAt,LocalDateTime recordedAt,String actor,String reason,String triggerType,String triggerId,String correlationId,Long reverseOfId,Integer schemaVersion,List<Change> changes){
 public LifecycleResponse{changes=List.copyOf(changes);}
 /** JSON前额/增量/后额和版本。 @author Punjab */
 public record Change(Long id,String objectType,Long objectId,JsonNode beforeSnapshot,JsonNode deltaSnapshot,JsonNode afterSnapshot,Long beforeVersion,Long afterVersion){}
}
