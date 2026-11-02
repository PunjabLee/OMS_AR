package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.OutboxEnqueueService;import com.punjab.oms.ar.dao.finance.OutboxDao;import com.punjab.oms.ar.domain.entity.finance.ArOutbox;
import com.punjab.oms.ar.common.utils.FinanceValues;import com.punjab.oms.ar.base.ServiceException;
import com.fasterxml.jackson.databind.ObjectMapper;import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import java.time.LocalDateTime;
/** 事件和完整快照同事务入队；任何失败均中止业务提交。 @author Punjab */
@Service @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
public class OutboxEnqueueServiceImpl implements OutboxEnqueueService {
 private final OutboxDao dao;private final FinanceValues values;private final ObjectMapper json;
 public void enqueue(Long eventId,Long legalEntityId,String actor){
  if(eventId==null || legalEntityId==null || legalEntityId<=0 || actor==null || actor.isBlank())throw new ServiceException("通知上下文无效");
  var event=dao.event(eventId);if(event==null || !actor.equals(event.getActor()))throw new ServiceException("通知事件不存在或操作人不一致");
  var payload=json.createObjectNode();payload.put("schemaVersion",1);payload.put("eventId",eventId);payload.put("legalEntityId",legalEntityId);payload.put("destination","FINANCE_EVENTS");payload.set("event",json.valueToTree(event));
  var changes=payload.putArray("changes");
  for(var change:dao.changes(eventId)){
   ObjectNode item=json.valueToTree(change);
   try{item.set("beforeSnapshot",change.getBeforeSnapshot()==null?json.nullNode():json.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(change.getBeforeSnapshot()));item.set("deltaSnapshot",change.getDeltaSnapshot()==null?json.nullNode():json.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(change.getDeltaSnapshot()));item.set("afterSnapshot",change.getAfterSnapshot()==null?json.nullNode():json.reader().with(com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readTree(change.getAfterSnapshot()));}
   catch(Exception e){throw new ServiceException("通知快照解析失败");}
   changes.add(item);
  }
  var value=values.initialize(new ArOutbox(),actor);value.setEventId(eventId);value.setLegalEntityId(legalEntityId);value.setDestination("FINANCE_EVENTS");value.setPayload(values.json(payload));value.setStatus("PENDING");value.setAttemptCount(0);value.setNextAttemptAt(LocalDateTime.now());value.setVersion(0L);
  if(!dao.create(value))throw new ServiceException("通知入队失败");
 }
}
