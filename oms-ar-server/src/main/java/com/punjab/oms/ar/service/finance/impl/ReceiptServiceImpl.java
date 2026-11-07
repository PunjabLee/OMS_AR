package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.service.finance.*;
import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.query.finance.ReceiptDetailQuery;
import com.punjab.oms.ar.domain.vo.finance.ReceiptResponse;
import com.punjab.oms.ar.domain.convert.finance.ReceiptConverter;
import com.punjab.oms.ar.dao.finance.ReceiptDao;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.base.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Validator;import lombok.RequiredArgsConstructor;import java.math.BigDecimal;import java.util.List;
/** 收款登记与余额查询，不生成额外实收。 @author Punjab */
@Service @RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {
 private final CashReceiptPosting cash;
 private final ReceiptDao dao;private final ReceiptConverter converter;private final FinanceJournalService journal;private final FinanceValues values;private final Validator validator;
 /** 核验实收金额与来源，初始化未分配收款，并将登记流水和幂等结果一并提交。 */
 @Transactional(rollbackFor=Exception.class)
 public Long create(ReceiptCreateRequest request){
  if(request==null || !validator.validate(request).isEmpty())throw new ServiceException("收款参数无效");
  CurrentUser.require(request.legalEntityId(),"ar:receipt:create");values.positiveMoney(request.currency(),request.amount());
  var ticket=journal.begin(new JournalBeginRequest(request.legalEntityId(),"ar:receipt:create","RECEIPT_CREATE",request.requestKey(),values.json(request)));
  if(ticket.resultReference()!=null)return ticket.resultReference();
  var receipt=values.initialize(converter.toEntity(request),ticket.actor());receipt.setAllocatedAmount(BigDecimal.ZERO);receipt.setVersion(0L);
  if(!dao.create(receipt))throw new ServiceException("收款保存失败");
  String snapshot=values.json(receipt);
  var changes=new java.util.ArrayList<JournalEventRequest.Change>();
  changes.add(new JournalEventRequest.Change("RECEIPT",receipt.getId(),null,snapshot,snapshot,null,0L));
  cash.deposit(receipt,request.cashAccountId(),ticket.actor(),changes);
  journal.append(new JournalEventRequest("RECEIPT_CREATED",ticket.actor(),"收款登记",request.sourceSystem(),request.sourceId(),"receipt:"+ticket.id(),null,changes),ticket);
  journal.complete(ticket,receipt.getId());return receipt.getId();
 }
 /** 校验主体读权限，返回实收原额、已分配额及当前可用额。 */
 public ReceiptResponse detail(ReceiptDetailQuery query){
  if(query==null || !validator.validate(query).isEmpty())throw new ServiceException("收款查询参数无效");
  CurrentUser.require(query.legalEntityId(),"ar:read");var receipt=dao.detail(converter.toDetailQueryBO(query));if(receipt==null)throw new ServiceException("收款不存在或不可访问");return converter.toResponse(receipt);
 }
}
