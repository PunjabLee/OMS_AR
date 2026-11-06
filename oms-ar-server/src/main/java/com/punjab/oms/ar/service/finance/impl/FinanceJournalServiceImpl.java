package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.FinanceJournalService;
import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.vo.finance.JournalTicket;
import com.punjab.oms.ar.domain.entity.finance.*;
import com.punjab.oms.ar.dao.finance.ReceivableDao;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.base.*;
import java.security.MessageDigest;import java.nio.charset.StandardCharsets;import java.util.HexFormat;import java.time.LocalDateTime;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;
/** 幂等和流水加入外层业务事务。 @author Punjab */
@Service @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY,rollbackFor=Exception.class)
public class FinanceJournalServiceImpl implements FinanceJournalService {
 private final com.punjab.oms.ar.common.utils.FinanceSnapshotJson snapshots;
 private final com.punjab.oms.ar.service.finance.OutboxEnqueueService outbox;private final ReceivableDao dao;private final FinanceValues values;
 /** 校验身份并预留幂等键，比对请求摘要后返回操作凭据。 */
 public JournalTicket begin(JournalBeginRequest request){
  String actor=CurrentUser.require(request.legalEntityId(),request.permission());
  var operation=values.initialize(new ArOperation(),actor);operation.setLegalEntityId(request.legalEntityId());operation.setCallerId(actor);
  operation.setOperationType(request.action());operation.setRequestKey(request.requestKey());
  String hash;try{hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(request.payload().getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new ServiceException("请求摘要生成失败");}
  operation.setPayloadHash(hash);var locked=dao.reserve(operation);
  if(locked==null || !hash.equals(locked.getPayloadHash()))throw new ServiceException("同一请求键的内容不一致");
  return new JournalTicket(locked.getId(),locked.getResultReference(),actor,request.legalEntityId());
 }
 /** 将业务结果引用写入幂等记录，供相同请求重放。 */
 public void complete(JournalTicket ticket,Long result){var operation=new ArOperation();operation.setId(ticket.id());operation.setResultReference(result);operation.setUpdatedBy(ticket.actor());operation.setUpdatedTime(LocalDateTime.now());if(!dao.complete(operation))throw new ServiceException("幂等结果保存失败");}
 /** 保存事件及对象前后变化，并在同一事务生成待派发通知。 */
 public Long append(JournalEventRequest request,JournalTicket ticket){
  if(!request.actor().equals(ticket.actor()))throw new ServiceException("流水操作人不一致");
  var event=values.initialize(new ArEvent(),request.actor());event.setEventType(request.type());event.setOccurredAt(LocalDateTime.now());event.setRecordedAt(LocalDateTime.now());
  event.setActor(request.actor());event.setReason(request.reason());event.setTriggerType(request.triggerType());event.setTriggerId(request.triggerId());event.setCorrelationId(request.correlationId());event.setReverseOfId(request.reverseOfId());event.setSchemaVersion(2);
  if(!dao.createEvent(event))throw new ServiceException("事件保存失败");
  for(var input:request.changes()){
   var change=values.initialize(new ArChange(),request.actor());change.setEventId(event.getId());change.setObjectType(input.objectType());change.setObjectId(input.objectId());change.setBeforeSnapshot(snapshots.normalize(input.beforeSnapshot()));change.setDeltaSnapshot(snapshots.normalize(input.deltaSnapshot()));change.setAfterSnapshot(snapshots.normalize(input.afterSnapshot()));change.setBeforeVersion(input.beforeVersion());change.setAfterVersion(input.afterVersion());
   if(!dao.createChange(change))throw new ServiceException("快照保存失败");
  }
  outbox.enqueue(event.getId(),ticket.legalEntityId(),ticket.actor());
  return event.getId();
 }
}
