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
 private final com.punjab.oms.ar.dao.finance.SettlementDao settlements;
 private final CashReceiptPosting cash;
 private final com.punjab.oms.ar.dao.finance.CashAccountDao cashAccounts;
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
 /** 锁定账户及原收款，确认核销、冻结和提现都已解除，再按原额扣回。 */
 @Transactional(rollbackFor=Exception.class,isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
 public Long reverseDeposit(CashDepositReverseRequest request) {
    if(request==null || !validator.validate(request).isEmpty()) throw new ServiceException("冲正请求无效");
    var ticket=journal.begin(new JournalBeginRequest(request.legalEntityId(),"ar:cash:deposit:reverse",
            "CASH_DEPOSIT_REVERSE",request.requestKey(),values.json(request)));
    if(ticket.resultReference()!=null) return ticket.resultReference();
    var account=cash.account(request.receiptId(),request.legalEntityId(),true);
    if(account==null) throw new ServiceException("原收款缺少充值批次");
    var receipt=settlements.receipt(request.receiptId(),request.legalEntityId(),true);
    var lot=cashAccounts.receiptLot(request.receiptId());
    if(receipt==null || cashAccounts.reversed(lot.getId()) || receipt.getAllocatedAmount().signum()!=0
            || lot.getFrozenAmount().signum()!=0 || lot.getWithdrawnAmount().signum()!=0)
        throw new ServiceException("原充值已冲正或存在核销、冻结、提现");
    if(!cashAccounts.markReversed(lot.getId(),ticket.id(),receipt.getAmount(),request.reason(),ticket.actor()))
        throw new ServiceException("充值冲正保存失败");
    Long originalEvent=cashAccounts.originalReceiptEvent(receipt.getId());
    if(originalEvent==null) throw new ServiceException("原收款登记流水缺失，请先完成历史映射");
    var change=cash.move(account,receipt.getAmount().negate(),ticket.actor());
    var snapshot=values.json(java.util.Map.of("lotId",lot.getId(),"receiptId",receipt.getId(),"amount",receipt.getAmount()));
    var reversal=new JournalEventRequest.Change("CASH_DEPOSIT_REVERSAL",cashAccounts.reversalId(lot.getId()),null,snapshot,snapshot,null,0L);
    journal.append(new JournalEventRequest("CASH_DEPOSIT_REVERSED",ticket.actor(),request.reason(),
            "RECEIPT",receipt.getId().toString(),"cash-deposit-reverse:"+ticket.id(),originalEvent,List.of(reversal,change)),ticket);
    journal.complete(ticket,receipt.getId());
    return receipt.getId();
 }

 /** 校验主体读权限，返回实收原额、已分配额及当前可用额。 */
 @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public ReceiptResponse detail(ReceiptDetailQuery query){
  if(query==null || !validator.validate(query).isEmpty())throw new ServiceException("收款查询参数无效");
  CurrentUser.require(query.legalEntityId(),"ar:read");var receipt=dao.detail(converter.toDetailQueryBO(query));if(receipt==null)throw new ServiceException("收款不存在或不可访问");var lot=cashAccounts.receiptLot(receipt.getId());
  return converter.toResponse(receipt,lot==null?null:lot.getCashAccountId(),
          lot==null?BigDecimal.ZERO:lot.getFrozenAmount(),lot==null?BigDecimal.ZERO:lot.getWithdrawnAmount(),
          lot!=null && cashAccounts.reversed(lot.getId()) ? receipt.getAmount() : BigDecimal.ZERO);
 }
}
