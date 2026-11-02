package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.ReceivableQueryService;
import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.domain.query.finance.*;
import com.punjab.oms.ar.domain.vo.finance.ReceivableResponse;
import com.punjab.oms.ar.domain.vo.finance.ReceivableResponse.Amounts;
import com.punjab.oms.ar.domain.entity.finance.*;
import com.punjab.oms.ar.domain.convert.finance.ReceivableConverter;
import com.punjab.oms.ar.dao.finance.ReceivableDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 多表详情和分页使用一致读快照，避免金额明细与汇总不一致。 @author Punjab */
@Service @RequiredArgsConstructor
public class ReceivableQueryServiceImpl implements ReceivableQueryService {
 private final ReceivableDao dao;
 private final ReceivableConverter converter;
 private final Validator validator;
 @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public ReceivableResponse detail(ReceivableDetailQuery query){
  validate(query);CurrentUser.require(query.legalEntityId(),"ar:read");
  var document=dao.document(converter.toDetailQueryBO(query));
  if(document==null)throw new ServiceException("应收单不存在或不可访问");
  var lines=dao.lines(document.getId());
  var sources=dao.sources(lines.stream().map(ArLine::getId).toList()).stream().collect(Collectors.toMap(ArSource::getLineId,Function.identity()));
  var exchangeBases=dao.exchangeBases(lines.stream().map(ArLine::getId).toList()).stream().collect(Collectors.toMap(ArExchangeBasis::getLineId,Function.identity()));
  var adjustments=dao.adjustmentLinks(lines.stream().map(ArLine::getId).toList()).stream().collect(Collectors.toMap(ArAdjustmentLink::getLineId,Function.identity()));
  var result=lines.stream().map(line->converter.toResponse(line,converter.toResponse(sources.get(line.getId())),converter.toResponse(exchangeBases.get(line.getId())),converter.toResponse(adjustments.get(line.getId())))).toList();
  var active=lines.stream().filter(line->"ACTIVE".equals(line.getStatus())).toList();
  String state;
  if("VOID".equals(document.getStatus()))state="VOID";
  else if(!active.isEmpty() && active.stream().allMatch(line->line.getOpenAmount().signum()==0))state="SETTLED";
  else if(active.stream().anyMatch(line->line.getSettledAmount().signum()!=0 || line.getTransferredAmount().signum()!=0))state="PARTIAL";
  else state="UNSETTLED";
  return new ReceivableResponse(converter.toResponse(document),state,result,sum(lines,"PRINCIPAL"),sum(lines,"OUTPUT_TAX"));
 }
 @Override @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public PageResult<ReceivableResponse.Document> page(ReceivablePageQuery query){
  validate(query);CurrentUser.require(query.getLegalEntityId(),"ar:read");
  var result=dao.page(converter.toPageQueryBO(query));
  return new PageResult<>(result.total(),result.page(),result.size(),result.records().stream().map(converter::toResponse).toList());
 }
 private void validate(Object query){if(query==null || !validator.validate(query).isEmpty())throw new ServiceException("查询参数无效");}
 private Amounts sum(List<ArLine> lines,String type){
  BigDecimal original=BigDecimal.ZERO,settled=BigDecimal.ZERO,transferred=BigDecimal.ZERO,open=BigDecimal.ZERO,invoiced=BigDecimal.ZERO;
  for(var line:lines){
   if(!type.equals(line.getAmountType()))continue;
   original=original.add(line.getReceivableAmount());settled=settled.add(line.getSettledAmount());
   transferred=transferred.add(line.getTransferredAmount());open=open.add(line.getOpenAmount());invoiced=invoiced.add(line.getInvoicedAmount());
  }
  return new Amounts(original,settled,transferred,open,invoiced);
 }
}
