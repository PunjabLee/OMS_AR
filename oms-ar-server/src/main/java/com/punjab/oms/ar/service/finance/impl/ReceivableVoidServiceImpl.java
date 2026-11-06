package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.*;import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.vo.finance.*;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.dao.finance.ReceivableVoidDao;import com.punjab.oms.ar.common.utils.FinanceValues;import com.punjab.oms.ar.config.InvoiceProperties;import com.punjab.oms.ar.base.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Validator;import java.util.*;import java.math.BigDecimal;import java.time.LocalDateTime;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;
/** 作废使用当前读：来源→单头→有序业务组→行，原额及税率保持不变。 @author Punjab */
@Service @RequiredArgsConstructor public class ReceivableVoidServiceImpl implements ReceivableVoidService {
 private final ReceivableVoidDao dao;private final FinanceJournalService journal;private final FinanceValues values;private final Validator validator;private final InvoiceProperties invoiceProperties;
 /** 同类型同税率共享额度。 @author Punjab */
 private record Bucket(String type,BigDecimal rate){}
 /** 按财务权限加载整单及来源，对有效明细统一执行作废。 */
 @Override @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public ReceivableVoidResponse voidDocument(ReceivableVoidRequest request){
  validate(request);var ticket=journal.begin(new JournalBeginRequest(request.legalEntityId(),"ar:void","VOID_DOCUMENT",request.requestKey(),values.json(request)));if(ticket.resultReference()!=null)return result(ticket.resultReference());
  if(dao.document(request.documentId(),request.legalEntityId(),false)==null)throw new ServiceException("应收单不存在或不可访问");
  var selected=dao.lines(request.documentId());if(selected.isEmpty())throw new ServiceException("应收单无明细");var sources=dao.sources(selected.stream().map(ArLine::getId).toList());var guards=new TreeMap<String,ArSourceGuard>();
  for(var s:sources){var guard=guard(request.legalEntityId(),s.getSourceSystem(),s.getSourceType(),s.getSourceId(),s.getSourceLineId(),ticket.actor());guards.put(guard.getSourceKey(),guard);}
  var changes=new ArrayList<JournalEventRequest.Change>();var locked=lockGuards(guards,changes);return execute(ticket,request.legalEntityId(),request.reason(),"FINANCE",selected,locked,changes);
 }
 /** 按来源系统授权锁定指定来源槽，作废匹配行并阻止迟到生成。 */
 @Override @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public ReceivableVoidResponse voidUpstream(UpstreamVoidRequest request){
  validate(request);var ticket=journal.begin(new JournalBeginRequest(request.legalEntityId(),"ar:void:upstream:"+request.sourceSystem(),"VOID_UPSTREAM",request.requestKey(),values.json(request)));if(ticket.resultReference()!=null)return result(ticket.resultReference());
  var guards=new TreeMap<String,ArSourceGuard>();for(var s:request.sources()){var guard=guard(request.legalEntityId(),request.sourceSystem(),s.sourceType(),s.sourceId(),s.sourceLineId(),ticket.actor());if(guards.put(guard.getSourceKey(),guard)!=null)throw new ServiceException("来源槽不能重复选择");}
  var changes=new ArrayList<JournalEventRequest.Change>();var locked=lockGuards(guards,changes);var selected=new TreeMap<Long,ArLine>();for(var g:locked)for(var line:dao.sourceLines(g))selected.put(line.getId(),line);
  return execute(ticket,request.legalEntityId(),request.reason(),"UPSTREAM",new ArrayList<>(selected.values()),locked,changes);
 }
 /** 锁定单头、组及明细，验证有效依赖后作废并封锁来源，记录作废事件。 */
 private ReceivableVoidResponse execute(JournalTicket ticket,Long scope,String reason,String mode,List<ArLine> selected,List<ArSourceGuard> guards,List<JournalEventRequest.Change> changes){
  var docs=new TreeMap<Long,ArDocument>();for(var id:selected.stream().map(ArLine::getDocumentId).distinct().sorted().toList()){var doc=dao.document(id,scope,true);if(doc==null)throw new ServiceException("应收单不存在或不可访问");docs.put(id,doc);}
  var groupIds=new TreeSet<Long>();for(var line:selected){groupIds.add(line.getGroupId());var link=dao.adjustment(line.getId());if(link!=null)groupIds.add(link.getRelatedGroupId());}
  var groups=new ArrayList<ArGroup>();for(var id:groupIds){var group=dao.lockGroup(id,scope);if(group==null)throw new ServiceException("业务组不存在或不可访问");groups.add(group);}
  var active=new TreeMap<Long,ArLine>();for(var old:selected.stream().sorted(Comparator.comparing(ArLine::getId)).toList()){var row=dao.lockLine(old.getId());if(row==null || !scope.equals(row.getLegalEntityId()) || !old.getGroupId().equals(row.getGroupId()))throw new ServiceException("应收明细已变化");if("ACTIVE".equals(row.getStatus()))active.put(row.getId(),row);}
  for(var line:active.values()){
   if(line.getSettledAmount().signum()!=0 || line.getTransferredAmount().signum()!=0 || line.getInvoicedAmount().signum()!=0 || dao.dependencies(line.getId())>0)throw new ServiceException("应收仍有有效核销、余额转入或开票关联，请先撤销关联");
   if(dao.dependents(line.getId()).stream().anyMatch(l->!active.containsKey(l.getId())))throw new ServiceException("原应收仍有关联的有效调整，请先作废调整应收");
  }
  checkInvoiceCapacity(groups,active.keySet());
  for(var line:active.values()){String before=values.json(line);long version=line.getVersion();var delta=line.getOpenAmount().negate();line.setOpenAmount(BigDecimal.ZERO);line.setStatus("VOID");line.setVersion(version+1);touch(line,ticket.actor());check(dao.voidLine(line,version));changes.add(new JournalEventRequest.Change("LINE",line.getId(),before,values.json(Map.of("status","VOID","openAmount",delta)),values.json(line),version,line.getVersion()));}
  Set<Long> affectedDocs=new HashSet<>();for(var line:active.values())affectedDocs.add(line.getDocumentId());
  for(var doc:docs.values())if(affectedDocs.contains(doc.getId())){
   String before=values.json(doc);long version=doc.getVersion();boolean allVoid=dao.lines(doc.getId()).stream().allMatch(l->"VOID".equals(l.getStatus()));
   if(allVoid){doc.setStatus("VOID");doc.setVoidReason(reason);doc.setVoidedBy(ticket.actor());doc.setVoidedTime(LocalDateTime.now());}
   doc.setVersion(version+1);touch(doc,ticket.actor());check(dao.updateDocument(doc,version));changes.add(new JournalEventRequest.Change("DOCUMENT",doc.getId(),before,values.json(Map.of("status",doc.getStatus())),values.json(doc),version,doc.getVersion()));
  }
  for(var guard:guards)if(!Boolean.TRUE.equals(guard.getBlocked())){String before=values.json(guard);long version=guard.getVersion();guard.setBlocked(true);guard.setVersion(version+1);touch(guard,ticket.actor());check(dao.blockGuard(guard,version));changes.add(new JournalEventRequest.Change("SOURCE_GUARD",guard.getId(),before,values.json(Map.of("blocked",true)),values.json(guard),version,guard.getVersion()));}
  long event=journal.append(new JournalEventRequest("RECEIVABLE_VOIDED",ticket.actor(),reason,mode,ticket.id().toString(),"void:"+ticket.id(),null,changes),ticket);journal.complete(ticket,event);return result(event);
 }
 /** 模拟移除目标行后的共享开票净额，阻止扩大额度缺口的作废。 */
 private void checkInvoiceCapacity(List<ArGroup> groups,Set<Long> removing){
  for(var group:groups){Map<Bucket,BigDecimal> before=new HashMap<>(),removed=new HashMap<>();for(var row:dao.invoiceGroup(group)){
   if("EXCHANGE_DIFFERENCE".equals(row.getBusinessType()) && (!invoiceProperties.isExchangeDifferenceEnabled() || row.getExchangeBasisId()==null))continue;
   var key=new Bucket(row.getAmountType(),row.getTaxRate()==null?null:row.getTaxRate().stripTrailingZeros());var net=row.getReceivableAmount().subtract(row.getInvoicedAmount());before.merge(key,net,BigDecimal::add);if(removing.contains(row.getId()))removed.merge(key,net,BigDecimal::add);
  }
  for(var entry:removed.entrySet()){var prior=before.get(entry.getKey());if(prior.subtract(entry.getValue()).compareTo(prior.min(BigDecimal.ZERO))<0)throw new ServiceException("作废将扩大业务组开票额度缺口，请先处理相关开票或调整");}}
 }
 /** 按稳定身份顺序初始化和锁定来源，形成初始化审计变化。 */
 private List<ArSourceGuard> lockGuards(SortedMap<String,ArSourceGuard> selected,List<JournalEventRequest.Change> changes){
  var locked=new ArrayList<ArSourceGuard>();for(var proposed:selected.values()){boolean initialized=dao.initializeGuard(proposed);var row=dao.lockGuard(proposed.getLegalEntityId(),proposed.getSourceKey());
   if(row==null || Boolean.TRUE.equals(row.getDeleteStatus()) || !proposed.getSourceSystem().equals(row.getSourceSystem()) || !proposed.getSourceType().equals(row.getSourceType()) || !proposed.getSourceId().equals(row.getSourceId()) || !Objects.equals(proposed.getSourceLineId(),row.getSourceLineId()))throw new ServiceException("来源身份锁不一致");
   if(initialized){String snapshot=values.json(row);changes.add(new JournalEventRequest.Change("SOURCE_GUARD",row.getId(),null,snapshot,snapshot,null,0L));}locked.add(row);
  }return locked;
 }
 /** 按主体及稳定来源身份构建防止迟到生成的锁记录。 */
 private ArSourceGuard guard(Long scope,String system,String type,String id,String line,String actor){var guard=values.initialize(new ArSourceGuard(),actor);guard.setLegalEntityId(scope);guard.setSourceSystem(system);guard.setSourceType(type);guard.setSourceId(id);guard.setSourceLineId(line);guard.setVersion(0L);guard.setBlocked(false);try{guard.setSourceKey(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(values.json(Arrays.asList(system,type,id,line)).getBytes(StandardCharsets.UTF_8))));}catch(Exception e){throw new ServiceException("来源摘要生成失败");}return guard;}
 /** 从作废事件变化中提取实际受影响的单头和明细标识。 */
 private ReceivableVoidResponse result(Long event){var changes=dao.changes(event);return new ReceivableVoidResponse(event,changes.stream().filter(c->"DOCUMENT".equals(c.getObjectType())).map(ArChange::getObjectId).distinct().sorted().toList(),changes.stream().filter(c->"LINE".equals(c.getObjectType())).map(ArChange::getObjectId).distinct().sorted().toList());}
 /** 更新变更记录的操作人与时间。 */
 private void touch(ArBaseEntity entity,String actor){entity.setUpdatedBy(actor);entity.setUpdatedTime(LocalDateTime.now());}
 /** 验证请求对象及字段约束，拒绝无效输入。 */
 private void validate(Object request){if(request==null || !validator.validate(request).isEmpty())throw new ServiceException("作废参数无效，必须填写原因");}
 /** 持久化或版本条件更新未命中时抛出异常，中止当前业务。 */
 private void check(boolean value){if(!value)throw new ServiceException("作废对象已变化，请重新查询");}
}
