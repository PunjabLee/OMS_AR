package com.punjab.oms.ar.service.finance.impl;

import com.punjab.oms.ar.service.finance.*;
import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.vo.finance.*;
import com.punjab.oms.ar.domain.entity.finance.*;
import com.punjab.oms.ar.dao.finance.SettlementDao;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.enums.AmountType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 收款核销统一事务：按组锁定、预览指纹校验、原分配逆向恢复。 @author Punjab */
@Service @RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {
 private final SettlementDao dao;
 private final AllocationService allocator;
 private final FinanceJournalService journal;
 private final FinanceValues values;
 private final Validator validator;
 /** 当前余额及对应预览，仅在当前调用内有效。 @author Punjab */
 private record State(ArReceipt receipt,List<ArLine> lines,SettlementPreviewResponse view){}

 @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public SettlementPreviewResponse preview(SettlementPreviewRequest request){
  validate(request);CurrentUser.require(request.legalEntityId(),"ar:settle");return load(request,false).view();
 }
 @Override @Transactional(rollbackFor=Exception.class)
 public Long confirm(SettlementConfirmRequest request){
  validate(request);
  var ticket=journal.begin(new JournalBeginRequest(request.preview().legalEntityId(),"ar:settle","SETTLEMENT_CONFIRM",request.requestKey(),values.json(request)));
  if(ticket.resultReference()!=null)return ticket.resultReference();
  State state=load(request.preview(),true);
  if(!state.view().fingerprint().equals(request.expectedFingerprint()))throw new ServiceException("余额或目标明细已变化，请重新预览确认");
  if(state.view().usedAmount().signum()==0)throw new ServiceException("没有可核销的应收余额");
  var receipt=state.receipt();
  ArSettlement settlement=values.initialize(new ArSettlement(),ticket.actor());
  settlement.setSettlementNo("ST-"+UUID.randomUUID());settlement.setCustomerId(receipt.getCustomerId());settlement.setLegalEntityId(receipt.getLegalEntityId());settlement.setCurrency(receipt.getCurrency());
  settlement.setSettlementType("RECEIPT");settlement.setReceiptId(receipt.getId());settlement.setAmount(state.view().usedAmount());
  settlement.setStrategy(request.preview().strategy().name());settlement.setPreviewFingerprint(state.view().fingerprint());settlement.setStatus("POSTED");settlement.setReason(request.reason());settlement.setVersion(0L);
  check(dao.create(settlement));
  var changes=new ArrayList<JournalEventRequest.Change>();changes.add(initial("SETTLEMENT",settlement));
  var byId=state.lines().stream().collect(Collectors.toMap(ArLine::getId,Function.identity()));
  int sequence=0;
  for(var input:state.view().allocations()){
   var allocation=values.initialize(new ArSettlementAllocation(),ticket.actor());allocation.setSettlementId(settlement.getId());allocation.setSequenceNo(++sequence);allocation.setPositiveLineId(input.lineId());allocation.setAmountType(input.amountType().name());allocation.setAmount(input.amount());
   check(dao.allocate(allocation));changes.add(initial("SETTLEMENT_ALLOCATION",allocation));
   changes.add(moveLine(byId.get(input.lineId()),input.amount(),ticket.actor()));
  }
  changes.add(moveReceipt(receipt,settlement.getAmount(),ticket.actor()));
  journal.append(new JournalEventRequest("RECEIPT_SETTLED",ticket.actor(),request.reason(),"SETTLEMENT",settlement.getId().toString(),"settlement:"+ticket.id(),null,changes));
  journal.complete(ticket,settlement.getId());return settlement.getId();
 }
 @Override @Transactional(rollbackFor=Exception.class)
 public Long reverse(SettlementReverseRequest request){
  validate(request);
  var ticket=journal.begin(new JournalBeginRequest(request.legalEntityId(),"ar:reverse","SETTLEMENT_REVERSE",request.requestKey(),values.json(request)));
  if(ticket.resultReference()!=null)return ticket.resultReference();
  var settlement=dao.settlement(request.settlementId(),request.legalEntityId(),true);
  if(settlement==null)throw new ServiceException("核销单不存在或不可访问");
  if("REVERSED".equals(settlement.getStatus())){journal.complete(ticket,settlement.getId());return settlement.getId();}
  if(!Set.of("RECEIPT","OFFSET").contains(settlement.getSettlementType()))throw new ServiceException("该核销类型尚未支持反核销");
  var receipt=settlement.getReceiptId()==null?null:dao.receipt(settlement.getReceiptId(),request.legalEntityId(),true);
  if("RECEIPT".equals(settlement.getSettlementType()) && receipt==null)throw new ServiceException("原收款不可访问");
  var allocations=dao.allocations(settlement.getId());
  if(allocations.isEmpty())throw new ServiceException("原核销分配记录缺失");
  var ids=allocations.stream().flatMap(x->java.util.stream.Stream.of(x.getPositiveLineId(),x.getNegativeLineId())).filter(Objects::nonNull).distinct().sorted().toList();
  var references=dao.linesByIds(ids,false);
  if(references.size()!=ids.size())throw new ServiceException("原应收明细缺失");
  var groupIds=references.stream().map(ArLine::getGroupId).distinct().sorted().toList();
  var groups=dao.groups(groupIds,request.legalEntityId(),true);
  if(groups.size()!=groupIds.size() || groups.stream().anyMatch(g->!settlement.getCustomerId().equals(g.getCustomerId()) || !settlement.getCurrency().equals(g.getCurrency())))throw new ServiceException("原核销业务组范围不一致");
  var locked=dao.linesByIds(ids,true).stream().collect(Collectors.toMap(ArLine::getId,Function.identity()));
  var changes=new ArrayList<JournalEventRequest.Change>();BigDecimal total=BigDecimal.ZERO;
  for(var allocation:allocations){
   var line=locked.get(allocation.getPositiveLineId());
   if(line==null || !"ACTIVE".equals(line.getStatus()))throw new ServiceException("原应收明细不可反核销");
   changes.add(moveLine(line,allocation.getAmount().negate(),ticket.actor()));
   if("OFFSET".equals(settlement.getSettlementType())){
    if(allocation.getNegativeLineId()==null)throw new ServiceException("原抵销负数明细缺失");
    changes.add(moveLine(locked.get(allocation.getNegativeLineId()),allocation.getAmount(),ticket.actor()));
   }
   total=total.add(allocation.getAmount());
  }
  if(total.compareTo(settlement.getAmount())!=0)throw new ServiceException("原核销分配金额不一致");
  if(receipt!=null)changes.add(moveReceipt(receipt,total.negate(),ticket.actor()));
  String before=values.json(settlement);long version=settlement.getVersion();settlement.setStatus("REVERSED");settlement.setVersion(version+1);touch(settlement,ticket.actor());check(dao.update(settlement,version));
  changes.add(new JournalEventRequest.Change("SETTLEMENT",settlement.getId(),before,values.json(Map.of("status","REVERSED")),values.json(settlement),version,settlement.getVersion()));
  Long originalEvent=dao.postedEvent(settlement.getId());if(originalEvent==null)throw new ServiceException("原核销事件缺失");
  journal.append(new JournalEventRequest("SETTLEMENT_REVERSED",ticket.actor(),request.reason(),"SETTLEMENT",settlement.getId().toString(),"reverse:"+ticket.id(),originalEvent,changes));
  journal.complete(ticket,settlement.getId());return settlement.getId();
 }
 private State load(SettlementPreviewRequest request,boolean lock){
  if(new HashSet<>(request.groupIds()).size()!=request.groupIds().size())throw new ServiceException("业务组不能重复选择");
  var receipt=dao.receipt(request.receiptId(),request.legalEntityId(),lock);if(receipt==null)throw new ServiceException("收款不存在或不可访问");
  values.positiveMoney(receipt.getCurrency(),request.amount());
  if(request.amount().compareTo(receipt.getAmount().subtract(receipt.getAllocatedAmount()))>0)throw new ServiceException("收款可用余额不足");
  checkGroups(dao.groups(request.groupIds(),request.legalEntityId(),lock),request.groupIds(),receipt);
  var lines=dao.groupLines(request.groupIds(),lock);
  var groups=request.groupIds().stream().map(id->new AllocationPreviewRequest.Group(id,lines.stream().filter(line->id.equals(line.getGroupId())).map(line->new AllocationPreviewRequest.Line(line.getId(),AmountType.valueOf(line.getAmountType()),line.getOpenAmount())).toList())).toList();
  var allocated=allocator.preview(new AllocationPreviewRequest(request.amount(),Currency.getInstance(receipt.getCurrency()).getDefaultFractionDigits(),request.strategy(),groups));
  String fingerprint=hash(values.json(List.of(request,receipt,lines)));
  Map<Long,Long> versions=lines.stream().collect(Collectors.toMap(ArLine::getId,ArLine::getVersion));
  return new State(receipt,lines,new SettlementPreviewResponse(receipt.getVersion(),versions,allocated.allocations(),request.amount().subtract(allocated.unallocatedAmount()),allocated.unallocatedAmount(),fingerprint));
 }
 private void checkGroups(List<ArGroup> groups,List<Long> ids,ArReceipt receipt){
  if(groups.size()!=ids.size() || groups.stream().anyMatch(group->!receipt.getCustomerId().equals(group.getCustomerId()) || !receipt.getCurrency().equals(group.getCurrency())))throw new ServiceException("业务组不存在或客户、币种不一致");
 }
 private JournalEventRequest.Change moveLine(ArLine line,BigDecimal amount,String actor){
  if(line==null || !"ACTIVE".equals(line.getStatus()))throw new ServiceException("应收明细不可核销");
  String before=values.json(line);long version=line.getVersion();
  var settled=line.getSettledAmount().add(amount);var open=line.getOpenAmount().subtract(amount);
  if((line.getReceivableAmount().signum()>0 && (settled.signum()<0 || open.signum()<0)) || (line.getReceivableAmount().signum()<0 && (settled.signum()>0 || open.signum()>0)))throw new ServiceException("应收余额不足或反核销金额不一致");
  line.setSettledAmount(settled);line.setOpenAmount(open);line.setVersion(version+1);touch(line,actor);check(dao.updateLine(line,version));
  return new JournalEventRequest.Change("LINE",line.getId(),before,values.json(Map.of("settledAmount",amount,"openAmount",amount.negate())),values.json(line),version,line.getVersion());
 }
 private JournalEventRequest.Change moveReceipt(ArReceipt receipt,BigDecimal amount,String actor){
  String before=values.json(receipt);long version=receipt.getVersion();var allocated=receipt.getAllocatedAmount().add(amount);
  if(allocated.signum()<0 || allocated.compareTo(receipt.getAmount())>0)throw new ServiceException("收款余额不足或反核销金额不一致");
  receipt.setAllocatedAmount(allocated);receipt.setVersion(version+1);touch(receipt,actor);check(dao.updateReceipt(receipt,version));
  return new JournalEventRequest.Change("RECEIPT",receipt.getId(),before,values.json(Map.of("allocatedAmount",amount)),values.json(receipt),version,receipt.getVersion());
 }
 private JournalEventRequest.Change initial(String type,ArBaseEntity value){String json=values.json(value);return new JournalEventRequest.Change(type,value.getId(),null,json,json,null,0L);}
 private void touch(ArBaseEntity value,String actor){value.setUpdatedBy(actor);value.setUpdatedTime(LocalDateTime.now());}
 private void validate(Object request){if(request==null || !validator.validate(request).isEmpty())throw new ServiceException("核销请求参数无效");}
 private void check(boolean value){if(!value)throw new ServiceException("余额已变化，请刷新后重试");}
 private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new ServiceException("预览摘要计算失败");}}
}
