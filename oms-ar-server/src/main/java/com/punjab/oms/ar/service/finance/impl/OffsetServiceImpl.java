package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.*;
import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.vo.finance.*;
import com.punjab.oms.ar.domain.entity.finance.*;
import com.punjab.oms.ar.dao.finance.*;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.enums.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
/** 正负应收抵销：持久化关联约束、按组锁定及确认前指纹验证。 @author Punjab */
@Service @RequiredArgsConstructor
public class OffsetServiceImpl implements OffsetService {
 private final SettlementDao dao;
 private final ReceivableDao receivables;
 private final FinanceJournalService journal;
 private final FinanceValues values;
 private final Validator validator;
 /** 本次事务读取的应收及预览。 @author Punjab */
 private record State(ArLine negative,List<ArLine> targets,OffsetPreviewResponse view){}
 /** 按原行优先及用户续抵选择排列目标，计算双方剩余余额及确认指纹。 */
 @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public OffsetPreviewResponse preview(OffsetPreviewRequest request){validate(request);CurrentUser.require(request.legalEntityId(),"ar:settle");return load(request,false).view();}
 /** 重新读取并锁定分配依据，核对预览指纹后写核销单、分配、余额变化及流水。 */
 @Override @Transactional(rollbackFor=Exception.class)
 public Long confirm(OffsetConfirmRequest request){
  validate(request);
  var ticket=journal.begin(new JournalBeginRequest(request.preview().legalEntityId(),"ar:settle","OFFSET_CONFIRM",request.requestKey(),values.json(request)));
  if(ticket.resultReference()!=null)return ticket.resultReference();
  var state=load(request.preview(),true);var view=state.view();var negative=state.negative();
  if(!view.fingerprint().equals(request.expectedFingerprint()))throw new ServiceException("余额或目标明细已变化，请重新预览确认");
  if(view.usedAmount().signum()==0)throw new ServiceException("没有可抵销的应收余额");
  var settlement=values.initialize(new ArSettlement(),ticket.actor());
  settlement.setSettlementNo("OF-"+UUID.randomUUID());settlement.setCustomerId(negative.getCustomerId());settlement.setLegalEntityId(negative.getLegalEntityId());settlement.setCurrency(negative.getCurrency());
  settlement.setSettlementType("OFFSET");settlement.setAmount(view.usedAmount());settlement.setStrategy(request.preview().strategy().name());settlement.setPreviewFingerprint(view.fingerprint());settlement.setStatus("POSTED");settlement.setReason(request.reason());settlement.setVersion(0L);check(dao.create(settlement));
  var changes=new ArrayList<JournalEventRequest.Change>();changes.add(initial("SETTLEMENT",settlement));int sequence=0;
  for(var input:view.allocations()){
   var allocation=values.initialize(new ArSettlementAllocation(),ticket.actor());allocation.setSettlementId(settlement.getId());allocation.setSequenceNo(++sequence);allocation.setPositiveLineId(input.lineId());allocation.setNegativeLineId(negative.getId());allocation.setAmountType(input.amountType().name());allocation.setAmount(input.amount());
   check(dao.allocate(allocation));changes.add(initial("SETTLEMENT_ALLOCATION",allocation));
   var target=state.targets().stream().filter(x->x.getId().equals(input.lineId())).findFirst().orElseThrow();changes.add(move(target,input.amount(),ticket.actor()));
  }
  changes.add(move(negative,view.usedAmount().negate(),ticket.actor()));
  journal.append(new JournalEventRequest("OFFSET_SETTLED",ticket.actor(),request.reason(),"SETTLEMENT",settlement.getId().toString(),"offset:"+ticket.id(),null,changes),ticket);
  journal.complete(ticket,settlement.getId());return settlement.getId();
 }
 /** 加载当前业务对象及关联分配依据，按调用模式锁定并计算预览。 */
 private State load(OffsetPreviewRequest request,boolean lock){
  if(request.strategy()!=AllocationStrategy.GROUP_TAX_FIRST)throw new ServiceException("该核销策略尚未实现");
  if(new HashSet<>(request.groupIds()).size()!=request.groupIds().size())throw new ServiceException("业务组不能重复选择");
  var negative=receivables.line(request.negativeLineId(),false);checkNegative(negative,request);
  var link=receivables.adjustmentLink(negative.getId());
  if(link!=null && !request.continueOtherTargets() && !request.groupIds().isEmpty())throw new ServiceException("继续抵销其他明细须明确确认");
  var targetGroups=new LinkedHashSet<Long>();if(link!=null)targetGroups.add(link.getRelatedGroupId());targetGroups.addAll(request.groupIds());
  var allGroups=new TreeSet<Long>(targetGroups);allGroups.add(negative.getGroupId());
  var groups=dao.groups(List.copyOf(allGroups),request.legalEntityId(),lock);
  for(var group:groups)if(!negative.getCustomerId().equals(group.getCustomerId()) || !negative.getCurrency().equals(group.getCurrency()))throw new ServiceException("业务组客户或币种不一致");
  if(groups.size()!=allGroups.size())throw new ServiceException("业务组不存在或不可访问");
  // 所有业务组锁取得后才锁明细，与创建及其他核销保持相同锁顺序。
  negative=receivables.line(request.negativeLineId(),lock);checkNegative(negative,request);
  values.positiveMoney(negative.getCurrency(),request.amount());
  if(request.amount().compareTo(negative.getOpenAmount().negate())>0)throw new ServiceException("负数应收可用余额不足");
  var lines=targetGroups.isEmpty()?List.<ArLine>of():dao.groupLines(List.copyOf(targetGroups),lock);
  var ordered=new ArrayList<ArLine>();
  if(link!=null && link.getOriginalLineId()!=null)for(var line:lines)if(line.getId().equals(link.getOriginalLineId()))ordered.add(line);
  for(Long groupId:targetGroups){
   var candidates=new ArrayList<ArLine>();
   for(var line:lines){
    if(!groupId.equals(line.getGroupId()) || ordered.contains(line))continue;
    if(link!=null && !line.getAmountType().equals(negative.getAmountType()))continue;
    if(link!=null && link.getOriginalLineId()!=null && !request.continueOtherTargets())continue;
    candidates.add(line);
   }
   candidates.sort(Comparator.comparingInt((ArLine x)->"OUTPUT_TAX".equals(x.getAmountType())?0:1).thenComparing(ArLine::getId));ordered.addAll(candidates);
  }
  var allocations=new ArrayList<AllocationPreviewResponse.Allocation>();var remaining=request.amount();
  for(var line:ordered){
   if(line.getOpenAmount().signum()<=0 || remaining.signum()==0)continue;
   if(link!=null && !line.getAmountType().equals(negative.getAmountType()))throw new ServiceException("关联原行金额类型不一致");
   var amount=remaining.min(line.getOpenAmount());allocations.add(new AllocationPreviewResponse.Allocation(line.getGroupId(),line.getId(),AmountType.valueOf(line.getAmountType()),amount));remaining=remaining.subtract(amount);
  }
  var used=request.amount().subtract(remaining);
  var fingerprint=hash(values.json(Arrays.asList(request,negative,link,lines)));
  return new State(negative,lines,new OffsetPreviewResponse(link!=null,link==null?null:link.getOriginalLineId(),negative.getVersion(),allocations,used,remaining,negative.getOpenAmount().negate().subtract(used),fingerprint));
 }
 /** 核验负数抵销来源的归属、有效状态与原额方向。 */
 private void checkNegative(ArLine line,OffsetPreviewRequest request){if(line==null || !request.legalEntityId().equals(line.getLegalEntityId()) || !"ACTIVE".equals(line.getStatus()) || line.getReceivableAmount().signum()>=0)throw new ServiceException("负数应收不存在或不可访问");}
 /** 按带符号增量更新目标行并生成前后快照。 */
 private JournalEventRequest.Change move(ArLine line,BigDecimal delta,String actor){
  String before=values.json(line);long version=line.getVersion();line.setSettledAmount(line.getSettledAmount().add(delta));line.setOpenAmount(line.getOpenAmount().subtract(delta));line.setVersion(version+1);line.setUpdatedBy(actor);line.setUpdatedTime(LocalDateTime.now());check(dao.updateLine(line,version));
  return new JournalEventRequest.Change("LINE",line.getId(),before,values.json(Map.of("settledAmount",delta,"openAmount",delta.negate())),values.json(line),version,line.getVersion());
 }
 /** 为新增业务对象生成初始化快照，前态为空。 */
 private JournalEventRequest.Change initial(String type,ArBaseEntity value){String json=values.json(value);return new JournalEventRequest.Change(type,value.getId(),null,json,json,null,0L);}
 /** 验证请求对象及字段约束，拒绝无效输入。 */
 private void validate(Object request){if(request==null || !validator.validate(request).isEmpty())throw new ServiceException("抵销请求参数无效");}
 /** 持久化或版本条件更新未命中时抛出异常，中止当前业务。 */
 private void check(boolean success){if(!success)throw new ServiceException("余额已变化，请重新预览");}
 /** 对序列化输入计算摘要，用于检测内容变化。 */
 private String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new ServiceException("预览摘要计算失败");}}
}
