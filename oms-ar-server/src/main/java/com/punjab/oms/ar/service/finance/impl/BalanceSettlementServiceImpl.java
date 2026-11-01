package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.*;import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.vo.finance.*;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.dao.finance.*;import com.punjab.oms.ar.common.utils.FinanceValues;import com.punjab.oms.ar.base.*;import com.punjab.oms.ar.enums.AmountType;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import jakarta.validation.Validator;import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;import java.time.LocalDateTime;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;import java.util.*;import java.util.stream.Collectors;import java.util.function.Function;
/** 账户及来源批次先锁定，再锁应收组；按原分配反核销。 @author Punjab */
@Service @RequiredArgsConstructor
public class BalanceSettlementServiceImpl implements BalanceSettlementService {
 private final BalanceSettlementDao sources;private final CustomerBalanceDao balances;private final SettlementDao settlements;
 private final AllocationService allocator;private final FinanceJournalService journal;private final FinanceValues values;private final Validator validator;
 /** 当前预览对应的事务内对象。 @author Punjab */
 private record State(ArCustomerAccount account,List<ArBalanceLot> lots,List<ArLine> lines,BalanceSettlementPreviewResponse view){}
 @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public BalanceSettlementPreviewResponse preview(BalanceSettlementPreviewRequest request){validate(request);CurrentUser.require(request.legalEntityId(),"ar:settle");return load(request,false).view();}
 @Override @Transactional(rollbackFor=Exception.class)
 public Long confirm(BalanceSettlementConfirmRequest request){
  validate(request);var ticket=journal.begin(new JournalBeginRequest(request.preview().legalEntityId(),"ar:settle","BALANCE_SETTLEMENT_CONFIRM",request.requestKey(),values.json(request)));if(ticket.resultReference()!=null)return ticket.resultReference();
  var state=load(request.preview(),true);var view=state.view();var account=state.account();
  if(!view.fingerprint().equals(request.expectedFingerprint()))throw new ServiceException("账户余额、来源批次或应收已变化，请重新预览");
  if(view.usedAmount().signum()==0)throw new ServiceException("没有可抵扣的应收余额");
  var settlement=values.initialize(new ArSettlement(),ticket.actor());settlement.setSettlementNo("BS-"+UUID.randomUUID());settlement.setCustomerId(account.getCustomerId());settlement.setLegalEntityId(account.getLegalEntityId());settlement.setCurrency(account.getCurrency());settlement.setSettlementType("BALANCE");settlement.setAmount(view.usedAmount());settlement.setStrategy(request.preview().strategy().name());settlement.setPreviewFingerprint(view.fingerprint());settlement.setStatus("POSTED");settlement.setReason(request.reason());settlement.setVersion(0L);check(settlements.create(settlement));
  var source=values.initialize(new ArBalanceSettlement(),ticket.actor());source.setSettlementId(settlement.getId());source.setAccountId(account.getId());check(sources.create(source));
  var changes=new ArrayList<JournalEventRequest.Change>();changes.add(initial("SETTLEMENT",settlement));changes.add(initial("BALANCE_SETTLEMENT",source));
  var lines=state.lines().stream().collect(Collectors.toMap(ArLine::getId,Function.identity()));var lots=state.lots().stream().collect(Collectors.toMap(ArBalanceLot::getId,Function.identity()));
  Map<Long,Long> allocationIds=new HashMap<>();int sequence=0;
  for(var input:view.allocations()){
   var allocation=values.initialize(new ArSettlementAllocation(),ticket.actor());allocation.setSettlementId(settlement.getId());allocation.setSequenceNo(++sequence);allocation.setPositiveLineId(input.lineId());allocation.setAmountType(input.amountType().name());allocation.setAmount(input.amount());check(settlements.allocate(allocation));allocationIds.put(input.lineId(),allocation.getId());changes.add(initial("SETTLEMENT_ALLOCATION",allocation));changes.add(moveLine(lines.get(input.lineId()),input.amount(),ticket.actor()));
  }
  sequence=0;Map<Long,BigDecimal> lotDeltas=new TreeMap<>();
  for(var input:view.lots()){
   var usage=values.initialize(new ArBalanceUsage(),ticket.actor());usage.setSettlementId(settlement.getId());usage.setAllocationId(allocationIds.get(input.lineId()));usage.setLotId(input.lotId());usage.setSequenceNo(++sequence);usage.setAmount(input.amount());check(sources.createUsage(usage));changes.add(initial("BALANCE_USAGE",usage));lotDeltas.merge(input.lotId(),input.amount(),BigDecimal::add);
  }
  for(var entry:lotDeltas.entrySet())changes.add(moveLot(lots.get(entry.getKey()),entry.getValue(),ticket.actor()));
  changes.add(moveAccount(account,view.usedAmount().negate(),ticket.actor()));
  journal.append(new JournalEventRequest("BALANCE_SETTLED",ticket.actor(),request.reason(),"SETTLEMENT",settlement.getId().toString(),"balance-settle:"+ticket.id(),null,changes));journal.complete(ticket,settlement.getId());return settlement.getId();
 }
 @Override @Transactional(rollbackFor=Exception.class)
 public Long reverse(SettlementReverseRequest request){
  validate(request);var ticket=journal.begin(new JournalBeginRequest(request.legalEntityId(),"ar:reverse","SETTLEMENT_REVERSE",request.requestKey(),values.json(request)));if(ticket.resultReference()!=null)return ticket.resultReference();
  var settlement=settlements.settlement(request.settlementId(),request.legalEntityId(),true);
  if(settlement==null || !"BALANCE".equals(settlement.getSettlementType()))throw new ServiceException("余额核销单不存在或不可访问");
  if("REVERSED".equals(settlement.getStatus())){journal.complete(ticket,settlement.getId());return settlement.getId();}
  var source=sources.source(settlement.getId());if(source==null)throw new ServiceException("原核销账户记录缺失");
  var account=balances.account(source.getAccountId(),request.legalEntityId(),true);
  if(account==null || !account.getCustomerId().equals(settlement.getCustomerId()) || !account.getCurrency().equals(settlement.getCurrency()))throw new ServiceException("原核销账户范围不一致");
  var usages=sources.usages(settlement.getId());var allocations=settlements.allocations(settlement.getId());
  if(usages.isEmpty() || allocations.isEmpty())throw new ServiceException("原核销分配缺失");
  var lotIds=usages.stream().map(ArBalanceUsage::getLotId).distinct().sorted().toList();var lots=sources.lots(lotIds,true);
  if(lots.size()!=lotIds.size() || lots.stream().anyMatch(l->!account.getId().equals(l.getAccountId()) || !"ACTIVE".equals(l.getStatus())))throw new ServiceException("原来源批次不可反核销");
  var lineIds=allocations.stream().map(ArSettlementAllocation::getPositiveLineId).distinct().sorted().toList();var references=settlements.linesByIds(lineIds,false);
  if(references.size()!=lineIds.size())throw new ServiceException("原应收明细缺失");
  var groupIds=references.stream().map(ArLine::getGroupId).distinct().sorted().toList();checkGroups(settlements.groups(groupIds,request.legalEntityId(),true),groupIds,account);
  var lines=settlements.linesByIds(lineIds,true).stream().collect(Collectors.toMap(ArLine::getId,Function.identity()));
  Map<Long,BigDecimal> usageByAllocation=new HashMap<>(),usageByLot=new TreeMap<>();
  for(var usage:usages){usageByAllocation.merge(usage.getAllocationId(),usage.getAmount(),BigDecimal::add);usageByLot.merge(usage.getLotId(),usage.getAmount(),BigDecimal::add);}
  var changes=new ArrayList<JournalEventRequest.Change>();BigDecimal total=BigDecimal.ZERO;
  for(var allocation:allocations){
   var used=usageByAllocation.remove(allocation.getId());if(used==null || used.compareTo(allocation.getAmount())!=0 || allocation.getNegativeLineId()!=null)throw new ServiceException("原批次与应收分配金额不一致");
   changes.add(moveLine(lines.get(allocation.getPositiveLineId()),allocation.getAmount().negate(),ticket.actor()));total=total.add(allocation.getAmount());
  }
  if(!usageByAllocation.isEmpty() || total.compareTo(settlement.getAmount())!=0)throw new ServiceException("原核销金额不一致");
  for(var lot:lots)changes.add(moveLot(lot,usageByLot.get(lot.getId()).negate(),ticket.actor()));changes.add(moveAccount(account,total,ticket.actor()));
  String before=values.json(settlement);long version=settlement.getVersion();settlement.setStatus("REVERSED");settlement.setVersion(version+1);touch(settlement,ticket.actor());check(settlements.update(settlement,version));changes.add(new JournalEventRequest.Change("SETTLEMENT",settlement.getId(),before,values.json(Map.of("status","REVERSED")),values.json(settlement),version,settlement.getVersion()));
  Long original=settlements.postedEvent(settlement.getId());if(original==null)throw new ServiceException("原核销事件缺失");
  journal.append(new JournalEventRequest("SETTLEMENT_REVERSED",ticket.actor(),request.reason(),"SETTLEMENT",settlement.getId().toString(),"reverse:"+ticket.id(),original,changes));journal.complete(ticket,settlement.getId());return settlement.getId();
 }
 private State load(BalanceSettlementPreviewRequest request,boolean lock){
  if(new HashSet<>(request.groupIds()).size()!=request.groupIds().size())throw new ServiceException("业务组不能重复选择");
  var account=balances.account(request.accountId(),request.legalEntityId(),lock);if(account==null)throw new ServiceException("客户信用账户不存在或不可访问");values.positiveMoney(account.getCurrency(),request.amount());
  if(request.amount().compareTo(account.getBalanceAmount())>0)throw new ServiceException("客户信用余额不足");
  var lots=sources.availableLots(account.getId(),lock);
  BigDecimal available=lots.stream().map(l->l.getAmount().subtract(l.getUsedAmount())).reduce(BigDecimal.ZERO,BigDecimal::add);
  if(available.compareTo(account.getBalanceAmount())!=0)throw new ServiceException("账户余额与来源批次不一致，请先对账");
  checkGroups(settlements.groups(request.groupIds(),request.legalEntityId(),lock),request.groupIds(),account);
  var lines=settlements.groupLines(request.groupIds(),lock);
  var groups=request.groupIds().stream().map(id->new AllocationPreviewRequest.Group(id,lines.stream().filter(l->id.equals(l.getGroupId())).map(l->new AllocationPreviewRequest.Line(l.getId(),AmountType.valueOf(l.getAmountType()),l.getOpenAmount())).toList())).toList();
  var allocated=allocator.preview(new AllocationPreviewRequest(request.amount(),Currency.getInstance(account.getCurrency()).getDefaultFractionDigits(),request.strategy(),groups));
  var lotAllocations=new ArrayList<BalanceSettlementPreviewResponse.LotAllocation>();int index=0;BigDecimal lotRemaining=lots.isEmpty()?BigDecimal.ZERO:lots.getFirst().getAmount().subtract(lots.getFirst().getUsedAmount());
  for(var allocation:allocated.allocations()){
   BigDecimal remaining=allocation.amount();
   while(remaining.signum()>0){
    if(index>=lots.size())throw new ServiceException("来源批次不足");
    if(lotRemaining.signum()==0){index++;if(index<lots.size())lotRemaining=lots.get(index).getAmount().subtract(lots.get(index).getUsedAmount());continue;}
    var amount=remaining.min(lotRemaining);lotAllocations.add(new BalanceSettlementPreviewResponse.LotAllocation(allocation.lineId(),lots.get(index).getId(),amount));remaining=remaining.subtract(amount);lotRemaining=lotRemaining.subtract(amount);
   }
  }
  String fingerprint=hash(values.json(List.of(request,account,lots,lines)));
  return new State(account,lots,lines,new BalanceSettlementPreviewResponse(account.getVersion(),allocated.allocations(),lotAllocations,request.amount().subtract(allocated.unallocatedAmount()),allocated.unallocatedAmount(),fingerprint));
 }
 private void checkGroups(List<ArGroup> groups,List<Long> ids,ArCustomerAccount account){if(groups.size()!=ids.size() || groups.stream().anyMatch(g->!account.getCustomerId().equals(g.getCustomerId()) || !account.getCurrency().equals(g.getCurrency())))throw new ServiceException("业务组不存在或客户、币种不一致");}
 private JournalEventRequest.Change moveLine(ArLine line,BigDecimal amount,String actor){
  if(line==null || !"ACTIVE".equals(line.getStatus()) || line.getReceivableAmount().signum()<=0)throw new ServiceException("应收明细不可核销");
  String before=values.json(line);long version=line.getVersion();var settled=line.getSettledAmount().add(amount);var open=line.getOpenAmount().subtract(amount);if(settled.signum()<0 || open.signum()<0)throw new ServiceException("应收余额不足或原分配不一致");
  line.setSettledAmount(settled);line.setOpenAmount(open);line.setVersion(version+1);touch(line,actor);check(settlements.updateLine(line,version));return new JournalEventRequest.Change("LINE",line.getId(),before,values.json(Map.of("settledAmount",amount,"openAmount",amount.negate())),values.json(line),version,line.getVersion());
 }
 private JournalEventRequest.Change moveLot(ArBalanceLot lot,BigDecimal amount,String actor){
  String before=values.json(lot);long version=lot.getVersion();var used=lot.getUsedAmount().add(amount);if(!"ACTIVE".equals(lot.getStatus()) || used.signum()<0 || used.compareTo(lot.getAmount())>0)throw new ServiceException("来源批次余额不足或原分配不一致");lot.setUsedAmount(used);lot.setVersion(version+1);touch(lot,actor);check(balances.updateLot(lot,version));return new JournalEventRequest.Change("BALANCE_LOT",lot.getId(),before,values.json(Map.of("usedAmount",amount)),values.json(lot),version,lot.getVersion());
 }
 private JournalEventRequest.Change moveAccount(ArCustomerAccount account,BigDecimal amount,String actor){
  String before=values.json(account);long version=account.getVersion();var balance=account.getBalanceAmount().add(amount);if(balance.signum()<0)throw new ServiceException("客户信用余额不足");account.setBalanceAmount(balance);account.setVersion(version+1);touch(account,actor);check(balances.updateAccount(account,version));return new JournalEventRequest.Change("CUSTOMER_ACCOUNT",account.getId(),before,values.json(Map.of("balanceAmount",amount)),values.json(account),version,account.getVersion());
 }
 private JournalEventRequest.Change initial(String type,ArBaseEntity value){String json=values.json(value);return new JournalEventRequest.Change(type,value.getId(),null,json,json,null,0L);}
 private void touch(ArBaseEntity value,String actor){value.setUpdatedBy(actor);value.setUpdatedTime(LocalDateTime.now());}
 private void validate(Object request){if(request==null || !validator.validate(request).isEmpty())throw new ServiceException("余额核销请求参数无效");}
 private void check(boolean ok){if(!ok)throw new ServiceException("余额已变化，请重试");}
 private String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new ServiceException("预览摘要计算失败");}}
}
