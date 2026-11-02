package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.*;import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.query.finance.*;import com.punjab.oms.ar.domain.vo.finance.*;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.convert.finance.InvoiceConverter;import com.punjab.oms.ar.dao.finance.InvoiceDao;import com.punjab.oms.ar.common.utils.FinanceValues;import com.punjab.oms.ar.base.*;import com.punjab.oms.ar.enums.InvoiceKind;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.*;import lombok.RequiredArgsConstructor;import jakarta.validation.Validator;
import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.*;import java.util.function.Function;import java.util.stream.Collectors;import java.nio.charset.StandardCharsets;import java.security.MessageDigest;
/** 登记采用READ_COMMITTED，取得组锁后重新读取共享额度；只更新已开票金额。 @author Punjab */
@Service @RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {
 private final InvoiceDao dao;private final InvoiceEligibilityService eligibility;private final InvoiceConverter converter;private final FinanceJournalService journal;private final FinanceValues values;private final Validator validator;
 /** 当前校验与可更新源行。 @author Punjab */
 private record State(List<ArInvoiceEligibilityLine> sources,InvoicePreviewResponse view){}
 /** 同一外部发票行、应收及原分配的唯一组合。 @author Punjab */
 private record Selection(String externalLine,Long lineId,Long originalId){}
 /** 与开票额度查询相同的共享额度键。 @author Punjab */
 private record Bucket(Long groupId,String amountType,BigDecimal rate){}
 @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public InvoicePreviewResponse preview(InvoicePreviewRequest request){validate(request);CurrentUser.require(request.legalEntityId(),"ar:invoice:write");return load(request,false).view();}
 @Override @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public Long register(InvoiceRegisterRequest request){
  validate(request);var ticket=journal.begin(new JournalBeginRequest(request.preview().legalEntityId(),"ar:invoice:write","INVOICE_REGISTER",request.requestKey(),values.json(request)));if(ticket.resultReference()!=null)return ticket.resultReference();
  var state=load(request.preview(),true);if(!state.view().fingerprint().equals(request.expectedFingerprint()))throw new ServiceException("应收、原发票或开票额度已变化，请重新预览");
  var input=request.preview();var invoice=values.initialize(new ArInvoice(),ticket.actor());invoice.setSourceSystem(request.sourceSystem());invoice.setSourceId(request.sourceId());invoice.setInvoiceNo(request.invoiceNo());invoice.setInvoiceDate(request.invoiceDate());invoice.setCustomerId(input.customerId());invoice.setLegalEntityId(input.legalEntityId());invoice.setCurrency(input.currency());invoice.setInvoiceKind(input.kind().name());invoice.setOriginalInvoiceId(input.originalInvoiceId());invoice.setTotalAmount(input.totalAmount());invoice.setStatus("ACTIVE");invoice.setVersion(0L);check(dao.create(invoice));
  var changes=new ArrayList<JournalEventRequest.Change>();changes.add(initial("INVOICE",invoice));Map<Long,BigDecimal> deltas=new TreeMap<>();
  for(var line:state.view().allocations()){
   var allocation=values.initialize(new ArInvoiceAllocation(),ticket.actor());allocation.setInvoiceId(invoice.getId());allocation.setInvoiceKind(invoice.getInvoiceKind());allocation.setInvoiceLineId(line.invoiceLineId());allocation.setReceivableLineId(line.receivableLineId());allocation.setAmountType(line.amountType());allocation.setAmount(line.amount());allocation.setTaxRate(line.taxRate());allocation.setOriginalInvoiceId(input.originalInvoiceId());allocation.setOriginalAllocationId(line.originalAllocationId());check(dao.allocate(allocation));changes.add(initial("INVOICE_ALLOCATION",allocation));deltas.merge(line.receivableLineId(),line.amount(),BigDecimal::add);
  }
  var rows=state.sources().stream().collect(Collectors.toMap(ArLine::getId,Function.identity()));for(var entry:deltas.entrySet())changes.add(move(rows.get(entry.getKey()),entry.getValue(),ticket.actor()));
  journal.append(new JournalEventRequest("INVOICE_REGISTERED",ticket.actor(),request.reason(),"INVOICE",invoice.getId().toString(),"invoice:"+ticket.id(),null,changes),ticket);journal.complete(ticket,invoice.getId());return invoice.getId();
 }
 @Override @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
 public Long reverse(InvoiceReverseRequest request){
  validate(request);var ticket=journal.begin(new JournalBeginRequest(request.legalEntityId(),"ar:reverse","INVOICE_REVERSE",request.requestKey(),values.json(request)));if(ticket.resultReference()!=null)return ticket.resultReference();
  var reference=dao.invoice(request.invoiceId(),request.legalEntityId(),false);if(reference==null)throw new ServiceException("发票关联不存在或不可访问");
  // 红字登记/撤销均先取得原蓝字锁，避免与蓝字撤销反向加锁。
  if("RED".equals(reference.getInvoiceKind())){var original=dao.invoice(reference.getOriginalInvoiceId(),request.legalEntityId(),true);if(original==null || !"BLUE".equals(original.getInvoiceKind()))throw new ServiceException("原蓝字关联缺失");}
  var invoice=dao.invoice(request.invoiceId(),request.legalEntityId(),true);if("REVERSED".equals(invoice.getStatus())){journal.complete(ticket,invoice.getId());return invoice.getId();}
  if("BLUE".equals(invoice.getInvoiceKind()) && dao.activeRedCount(invoice.getId())>0)throw new ServiceException("原蓝字存在有效红字关联，请先撤销红字关联");
  var allocations=dao.allocations(invoice.getId());if(allocations.isEmpty())throw new ServiceException("原发票分配缺失");
  var ids=allocations.stream().map(ArInvoiceAllocation::getReceivableLineId).distinct().sorted().toList();var rows=lockedSources(ids,invoice.getLegalEntityId(),invoice.getCustomerId(),invoice.getCurrency(),true);
  var byId=rows.stream().collect(Collectors.toMap(ArLine::getId,Function.identity()));Map<Long,BigDecimal> deltas=new TreeMap<>();BigDecimal total=BigDecimal.ZERO;
  for(var allocation:allocations){deltas.merge(allocation.getReceivableLineId(),allocation.getAmount().negate(),BigDecimal::add);total=total.add(allocation.getAmount());}
  if(total.compareTo(invoice.getTotalAmount())!=0)throw new ServiceException("原发票分配合计不一致");
  var changes=new ArrayList<JournalEventRequest.Change>();for(var entry:deltas.entrySet())changes.add(move(byId.get(entry.getKey()),entry.getValue(),ticket.actor()));
  String before=values.json(invoice);long version=invoice.getVersion();invoice.setStatus("REVERSED");invoice.setVersion(version+1);touch(invoice,ticket.actor());check(dao.update(invoice,version));changes.add(new JournalEventRequest.Change("INVOICE",invoice.getId(),before,values.json(Map.of("status","REVERSED")),values.json(invoice),version,invoice.getVersion()));
  Long originalEvent=dao.postedEvent(invoice.getId());if(originalEvent==null)throw new ServiceException("原开票关联事件缺失");
  journal.append(new JournalEventRequest("INVOICE_REVERSED",ticket.actor(),request.reason(),"INVOICE",invoice.getId().toString(),"invoice-reverse:"+ticket.id(),originalEvent,changes),ticket);journal.complete(ticket,invoice.getId());return invoice.getId();
 }
 @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public InvoiceResponse detail(InvoiceDetailQuery query){
  validate(query);CurrentUser.require(query.legalEntityId(),"ar:read");var invoice=dao.detail(converter.toDetailQueryBO(query));if(invoice==null)throw new ServiceException("发票关联不存在或不可访问");var allocations=dao.allocations(invoice.getId());Map<Long,BigDecimal> red=redAmounts(dao.activeRedAllocations(invoice.getId()));boolean blue="BLUE".equals(invoice.getInvoiceKind()) && "ACTIVE".equals(invoice.getStatus());
  return new InvoiceResponse(converter.toResponse(invoice),allocations.stream().map(a->converter.toResponse(a,blue?a.getAmount().add(red.getOrDefault(a.getId(),BigDecimal.ZERO)):null)).toList());
 }
 private State load(InvoicePreviewRequest request,boolean lock){
  boolean red=request.kind()==InvoiceKind.RED;if(red!=(request.originalInvoiceId()!=null) || request.totalAmount().signum()!=(red?-1:1))throw new ServiceException("发票方向、金额或原蓝字引用无效");values.positiveMoney(request.currency(),request.totalAmount().abs());
  ArInvoice original=null;List<ArInvoiceAllocation> originalAllocations=List.of(),priorRed=List.of();List<ArInvoiceEligibilityLine> originalSources=List.of();
  if(red){
   original=dao.invoice(request.originalInvoiceId(),request.legalEntityId(),lock);
   if(original==null || !"BLUE".equals(original.getInvoiceKind()) || !"ACTIVE".equals(original.getStatus()) || !request.customerId().equals(original.getCustomerId()) || !request.currency().equals(original.getCurrency()))throw new ServiceException("原蓝字关联不存在、已撤销或结算范围不一致");
   originalAllocations=dao.allocations(original.getId());priorRed=dao.activeRedAllocations(original.getId());originalSources=dao.sources(originalAllocations.stream().map(ArInvoiceAllocation::getReceivableLineId).distinct().sorted().toList(),request.legalEntityId());
  }
  var ids=request.lines().stream().map(InvoicePreviewRequest.Line::receivableLineId).distinct().sorted().toList();var rows=lockedSources(ids,request.legalEntityId(),request.customerId(),request.currency(),lock);
  var groupIds=rows.stream().map(ArInvoiceEligibilityLine::getInvoiceGroupId).distinct().sorted().toList();var quota=eligibility.forRegistration(new InvoiceEligibilityQuery(request.legalEntityId(),request.customerId(),request.currency(),groupIds));
  var rowMap=rows.stream().collect(Collectors.toMap(ArLine::getId,Function.identity()));var quotaLines=quota.lines().stream().collect(Collectors.toMap(InvoiceEligibilityResponse.Line::id,Function.identity()));var originals=originalAllocations.stream().collect(Collectors.toMap(ArInvoiceAllocation::getId,Function.identity()));var originalRows=originalSources.stream().collect(Collectors.toMap(ArLine::getId,Function.identity()));var redUsed=redAmounts(priorRed);
  Map<Long,BigDecimal> lineTotals=new HashMap<>(),originalTotals=new HashMap<>();Map<Bucket,BigDecimal> bucketTotals=new HashMap<>();Set<Selection> unique=new HashSet<>();var result=new ArrayList<InvoicePreviewResponse.Allocation>();BigDecimal total=BigDecimal.ZERO;
  for(var input:request.lines()){
   if(!unique.add(new Selection(input.invoiceLineId(),input.receivableLineId(),input.originalAllocationId())))throw new ServiceException("相同发票行、应收和原分配重复");
   var row=rowMap.get(input.receivableLineId());var capacity=quotaLines.get(input.receivableLineId());if(capacity==null || !capacity.invoiceSupported() || !row.getVersion().equals(capacity.version()))throw new ServiceException("应收不可开票或额度已变化");
   if(input.amount().signum()!=(red?-1:1) || row.getReceivableAmount().signum()!=(red?-1:1) || red!=(input.originalAllocationId()!=null))throw new ServiceException("发票分配与应收正负方向不一致");values.positiveMoney(request.currency(),input.amount().abs());
   if(!FinanceValues.sameTaxRate(input.taxRate(),row.getTaxRate()))throw new ServiceException("拟开票税率与原应收不一致，请先生成调整应收");
   if(red){
    var allocation=originals.get(input.originalAllocationId());if(allocation==null || !allocation.getAmountType().equals(row.getAmountType()) || !FinanceValues.sameTaxRate(allocation.getTaxRate(),row.getTaxRate()))throw new ServiceException("原蓝字分配不存在或价税类型、税率不一致");
    var originalRow=originalRows.get(allocation.getReceivableLineId());if(originalRow==null)throw new ServiceException("原蓝字应收不存在");
    if(row.getRelatedGroupId()!=null && !row.getRelatedGroupId().equals(originalRow.getGroupId()))throw new ServiceException("红字原分配不属于调整关联业务组");
    if(row.getAdjustmentOriginalLineId()!=null && !row.getAdjustmentOriginalLineId().equals(allocation.getReceivableLineId()))throw new ServiceException("红字原分配不是调整指定的原应收行");originalTotals.merge(allocation.getId(),input.amount().abs(),BigDecimal::add);
   }
   lineTotals.merge(row.getId(),input.amount().abs(),BigDecimal::add);bucketTotals.merge(bucket(row.getInvoiceGroupId(),row.getAmountType(),row.getTaxRate()),input.amount(),BigDecimal::add);total=total.add(input.amount());result.add(new InvoicePreviewResponse.Allocation(input.invoiceLineId(),row.getId(),row.getInvoiceGroupId(),row.getAmountType(),input.amount(),row.getTaxRate(),row.getVersion(),input.originalAllocationId()));
  }
  if(total.compareTo(request.totalAmount())!=0)throw new ServiceException("发票合计与应收分配合计不一致");
  for(var entry:lineTotals.entrySet()){var row=rowMap.get(entry.getKey());if(entry.getValue().compareTo(row.getReceivableAmount().subtract(row.getInvoicedAmount()).abs())>0)throw new ServiceException("超过应收行剩余开票额度");}
  if(!red){Map<Bucket,BigDecimal> available=new HashMap<>();for(var b:quota.buckets())available.put(bucket(b.groupId(),b.amountType(),b.taxRate()),b.blueAvailableAmount());for(var entry:bucketTotals.entrySet())if(entry.getValue().compareTo(available.getOrDefault(entry.getKey(),BigDecimal.ZERO))>0)throw new ServiceException("超过调整后的业务组共享开票额度");}
  else for(var entry:originalTotals.entrySet()){var remaining=originals.get(entry.getKey()).getAmount().add(redUsed.getOrDefault(entry.getKey(),BigDecimal.ZERO));if(remaining.signum()<0 || entry.getValue().compareTo(remaining)>0)throw new ServiceException("超过原蓝字分配剩余可红字金额");}
  String fingerprint=hash(values.json(Arrays.asList(request,quota.fingerprint(),original,originalAllocations,priorRed,originalSources)));return new State(rows,new InvoicePreviewResponse(request.kind(),total,result,fingerprint));
 }
 private List<ArInvoiceEligibilityLine> lockedSources(List<Long> ids,Long scope,Long customer,String currency,boolean lock){
  var rows=dao.sources(ids,scope);checkSources(rows,ids,customer,currency);var groups=rows.stream().map(ArInvoiceEligibilityLine::getInvoiceGroupId).distinct().sorted().toList();
  if(lock){var locked=dao.lockGroups(groups,scope);if(locked.size()!=groups.size() || locked.stream().anyMatch(g->!customer.equals(g.getCustomerId()) || !currency.equals(g.getCurrency())))throw new ServiceException("开票业务组不存在或范围不一致");rows=dao.sources(ids,scope);checkSources(rows,ids,customer,currency);if(!groups.equals(rows.stream().map(ArInvoiceEligibilityLine::getInvoiceGroupId).distinct().sorted().toList()))throw new ServiceException("应收开票归属已变化");}return rows;
 }
 private void checkSources(List<ArInvoiceEligibilityLine> rows,List<Long> ids,Long customer,String currency){if(rows.size()!=ids.size() || rows.stream().anyMatch(r->!customer.equals(r.getCustomerId()) || !currency.equals(r.getCurrency())))throw new ServiceException("应收不存在、已作废或结算范围不一致");}
 private Map<Long,BigDecimal> redAmounts(List<ArInvoiceAllocation> rows){Map<Long,BigDecimal> result=new HashMap<>();for(var row:rows)result.merge(row.getOriginalAllocationId(),row.getAmount(),BigDecimal::add);return result;}
 private Bucket bucket(Long group,String type,BigDecimal rate){return new Bucket(group,type,rate==null?null:rate.stripTrailingZeros());}
 private JournalEventRequest.Change move(ArLine line,BigDecimal amount,String actor){
  String before=values.json(line);long version=line.getVersion();var invoiced=line.getInvoicedAmount().add(amount);if(invoiced.abs().compareTo(line.getReceivableAmount().abs())>0 || (invoiced.signum()!=0 && invoiced.signum()!=line.getReceivableAmount().signum()))throw new ServiceException("应收已开票金额方向或额度不一致");line.setInvoicedAmount(invoiced);line.setVersion(version+1);touch(line,actor);check(dao.updateLine(line,version));return new JournalEventRequest.Change("LINE",line.getId(),before,values.json(Map.of("invoicedAmount",amount)),values.json(line),version,line.getVersion());
 }
 private JournalEventRequest.Change initial(String type,ArBaseEntity value){String json=values.json(value);return new JournalEventRequest.Change(type,value.getId(),null,json,json,null,0L);}
 private void touch(ArBaseEntity value,String actor){value.setUpdatedBy(actor);value.setUpdatedTime(LocalDateTime.now());}
 private void validate(Object request){if(request==null || !validator.validate(request).isEmpty())throw new ServiceException("发票关联参数无效");}
 private void check(boolean success){if(!success)throw new ServiceException("发票关联或余额已变化，请重新查询");}
 private String hash(String text){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new ServiceException("开票预览摘要失败");}}
}
