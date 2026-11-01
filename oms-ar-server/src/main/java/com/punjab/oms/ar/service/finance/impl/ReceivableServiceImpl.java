package com.punjab.oms.ar.service.finance.impl;

import com.punjab.oms.ar.service.finance.ReceivableService;
import com.punjab.oms.ar.domain.dto.finance.ReceivableCreateRequest;
import com.punjab.oms.ar.domain.entity.finance.*;
import com.punjab.oms.ar.domain.convert.finance.ReceivableConverter;
import com.punjab.oms.ar.dao.finance.ReceivableDao;
import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.enums.AmountType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** 应收、来源、幂等和快照的统一创建事务。 @author Punjab */
@Service @RequiredArgsConstructor
public class ReceivableServiceImpl implements ReceivableService {
 private final ReceivableDao dao;
 private final ReceivableConverter converter;
 private final ObjectMapper json;
 private final Validator validator;
 private final com.punjab.oms.ar.common.utils.ExchangeDifferenceCalculator exchangeCalculator;

 @Override @Transactional(rollbackFor=Exception.class)
 public Long create(ReceivableCreateRequest request){
  if(request==null || !validator.validate(request).isEmpty())throw new ServiceException("应收创建参数无效");
  String actor=CurrentUser.require(request.legalEntityId(),"ar:create");
  validateAmounts(request);
  var now=LocalDateTime.now();
  ArOperation operation=audit(new ArOperation(),actor,now);
  operation.setLegalEntityId(request.legalEntityId());operation.setCallerId(actor);
  operation.setOperationType("CREATE");operation.setRequestKey(request.requestKey());operation.setPayloadHash(hash(serialize(request)));
  ArOperation locked=dao.reserve(operation);
  if(locked==null)throw new ServiceException("无法取得幂等请求");
  if(!locked.getPayloadHash().equals(operation.getPayloadHash()))throw new ServiceException("同一请求键的内容不一致");
  if(locked.getResultReference()!=null)return locked.getResultReference();

  Map<Long,ArLine> originals=new HashMap<>();
  for(var input:request.lines())if(input.originalLineId()!=null){
   if(input.amount().signum()>=0)throw new ServiceException("只有负数应收可指定原应收行");
   ArLine original=dao.line(input.originalLineId(),false);
   if(original==null)throw new ServiceException("原应收行不存在");
   originals.put(original.getId(),original);
  }
  // 既有组统一按ID加锁，避免不同请求以相反顺序锁定多组。
  Map<Long,ArGroup> existing=new HashMap<>();
  java.util.stream.Stream.concat(request.lines().stream().map(ReceivableCreateRequest.Line::groupId),originals.values().stream().map(ArLine::getGroupId)).filter(Objects::nonNull).distinct().sorted().forEach(id->{
   ArGroup group=dao.group(id);
   if(group==null || !request.customerId().equals(group.getCustomerId()) || !request.legalEntityId().equals(group.getLegalEntityId()) || !request.currency().equals(group.getCurrency()))
    throw new ServiceException("业务组不存在或结算范围不一致");
   existing.put(id,group);
  });
  for(var input:request.lines())if(input.originalLineId()!=null){
   var original=dao.line(input.originalLineId(),true);
   if(original==null || !"ACTIVE".equals(original.getStatus()) || original.getReceivableAmount().signum()<=0
     || !request.customerId().equals(original.getCustomerId()) || !request.legalEntityId().equals(original.getLegalEntityId())
     || !request.currency().equals(original.getCurrency()) || !input.amountType().name().equals(original.getAmountType())
     || (input.groupId()!=null && !input.groupId().equals(original.getGroupId())))throw new ServiceException("原应收行的结算范围、金额类型或业务组不一致");
   originals.put(original.getId(),original);
  }
  ArDocument document=audit(converter.toEntity(request),actor,now);
  document.setDocNo("AR-"+UUID.randomUUID());document.setStatus("ACTIVE");document.setVersion(0L);
  document.setGenerationMode(request.lines().stream().allMatch(line->line.source()==null)?"DIRECT":"UPSTREAM");
  persist(dao.createDocument(document));
  ArEvent event=audit(new ArEvent(),actor,now);
  event.setEventType("CREATED");event.setOccurredAt(now);event.setRecordedAt(now);event.setActor(actor);
  event.setReason(request.reason());event.setCorrelationId("create:"+locked.getId());event.setSchemaVersion(1);
  event.setTriggerType(document.getGenerationMode());event.setTriggerId(request.requestKey());persist(dao.createEvent(event));
  snapshot(event,"DOCUMENT",document,actor,now);
  Map<String,ArGroup> newGroups=new HashMap<>();
  int sequence=0;
  for(var input:request.lines()){
   ArGroup group;
   if(input.groupId()!=null)group=existing.get(input.groupId());
   else {
    group=newGroups.get(input.groupKey());
    if(group==null){
     group=audit(new ArGroup(),actor,now);group.setGroupNo("GR-"+UUID.randomUUID());
     group.setCustomerId(request.customerId());group.setLegalEntityId(request.legalEntityId());group.setCurrency(request.currency());
     group.setBusinessDate(request.businessDate());group.setVersion(0L);persist(dao.createGroup(group));
     newGroups.put(input.groupKey(),group);snapshot(event,"GROUP",group,actor,now);
    }
   }
   ArLine line=audit(converter.toEntity(input),actor,now);
   line.setDocumentId(document.getId());line.setGroupId(group.getId());line.setLineNo(++sequence);
   line.setCustomerId(request.customerId());line.setLegalEntityId(request.legalEntityId());line.setCurrency(request.currency());
   line.setSettledAmount(BigDecimal.ZERO);line.setTransferredAmount(BigDecimal.ZERO);line.setInvoicedAmount(BigDecimal.ZERO);
   line.setOpenAmount(input.amount());line.setStatus("ACTIVE");line.setVersion(0L);
   persist(dao.createLine(line));snapshot(event,"LINE",line,actor,now);
   if(input.amount().signum()<0 && (input.originalLineId()!=null || input.groupId()!=null)){
    var link=audit(new ArAdjustmentLink(),actor,now);link.setLineId(line.getId());link.setOriginalLineId(input.originalLineId());
    link.setRelatedGroupId(input.originalLineId()!=null?originals.get(input.originalLineId()).getGroupId():input.groupId());
    persist(dao.createAdjustmentLink(link));snapshot(event,"ADJUSTMENT_LINK",link,actor,now);
   }
   if(input.exchangeBasis()!=null){
    var basis=input.exchangeBasis();var calculated=exchangeCalculator.calculate(basis,request.currency());var record=audit(new ArExchangeBasis(),actor,now);
    record.setLineId(line.getId());record.setOriginalCurrency(basis.originalCurrency());record.setOriginalAmount(basis.originalAmount());record.setOldRate(basis.oldRate());record.setNewRate(basis.newRate());record.setRateDirection(basis.rateDirection().name());record.setOldSettlementAmount(calculated.oldSettlementAmount());record.setNewSettlementAmount(calculated.newSettlementAmount());record.setConfirmedDifference(calculated.difference());
    persist(dao.createExchangeBasis(record));snapshot(event,"EXCHANGE_BASIS",record,actor,now);
   }
   if(input.source()!=null){
    var source=input.source();ArSource record=audit(new ArSource(),actor,now);
    record.setLineId(line.getId());record.setSourceSystem(source.system());record.setSourceType(source.type());
    record.setSourceNo(source.number());record.setSourceId(source.id());record.setSourceLineId(source.lineId());
    record.setOriginalLineId(input.originalLineId());
    record.setSourceEventId(source.eventId());record.setSourceOutputKey(source.outputKey());
    persist(dao.createSource(record));snapshot(event,"SOURCE",record,actor,now);
   }
  }
  locked.setResultReference(document.getId());locked.setUpdatedTime(now);locked.setUpdatedBy(actor);
  persist(dao.complete(locked));return document.getId();
 }
 private void validateAmounts(ReceivableCreateRequest request){
  int scale;
  try{scale=Currency.getInstance(request.currency()).getDefaultFractionDigits();}
  catch(IllegalArgumentException exception){throw new ServiceException("结算币种无效");}
  if(scale<0 || scale>8)throw new ServiceException("结算币种精度不支持");
  Map<String,Long> groupReferences=new HashMap<>();
  for(var line:request.lines()){
   if(line==null || line.amount().signum()==0)throw new ServiceException("应收明细不能为空或零金额");
   try{line.amount().setScale(scale,RoundingMode.UNNECESSARY);}
   catch(ArithmeticException exception){throw new ServiceException("金额超出币种精度");}
   if(line.amountType()==AmountType.OUTPUT_TAX && (line.taxRate()==null || line.taxBaseAmount()==null))
    throw new ServiceException("销项税行必须提供税率和计税依据");
   if("EXCHANGE_DIFFERENCE".equals(request.businessType()) && line.amountType()!=AmountType.PRINCIPAL)
    throw new ServiceException("汇率差异必须归为价款");
   if("EXCHANGE_DIFFERENCE".equals(request.businessType())){
    var calculated=exchangeCalculator.calculate(line.exchangeBasis(),request.currency());if(calculated.difference().compareTo(line.amount())!=0)throw new ServiceException("汇率差额与结算币种应收金额不一致");
   }else if(line.exchangeBasis()!=null)throw new ServiceException("仅汇率差异应收允许携带汇率依据");
   if(groupReferences.containsKey(line.groupKey()) && !Objects.equals(groupReferences.get(line.groupKey()),line.groupId()))
    throw new ServiceException("同一业务组键的关联目标不一致");
   groupReferences.put(line.groupKey(),line.groupId());
  }
 }
 private <T extends ArBaseEntity>T audit(T value,String actor,LocalDateTime now){
  value.setCreatedBy(actor);value.setUpdatedBy(actor);value.setCreatedTime(now);value.setUpdatedTime(now);value.setDeleteStatus(false);return value;
 }
 private void persist(boolean success){if(!success)throw new ServiceException("应收记录保存失败");}
 private void snapshot(ArEvent event,String type,ArBaseEntity value,String actor,LocalDateTime now){
  ArChange change=audit(new ArChange(),actor,now);change.setEventId(event.getId());change.setObjectType(type);change.setObjectId(value.getId());
  change.setDeltaSnapshot(serialize(value));change.setAfterSnapshot(serialize(value));change.setAfterVersion(0L);persist(dao.createChange(change));
 }
 private String serialize(Object value){try{return json.writeValueAsString(value);}catch(Exception exception){throw new ServiceException("快照序列化失败");}}
 private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception exception){throw new ServiceException("请求摘要生成失败");}}
}
