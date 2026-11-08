package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.service.finance.*;import com.punjab.oms.ar.dao.finance.*;import com.punjab.oms.ar.common.utils.FinanceValues;import com.punjab.oms.ar.base.*;
import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import lombok.RequiredArgsConstructor;import jakarta.validation.Validator;import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.*;
/** 转入信用余额不计入收款或已核销金额；来源批次使用后必须先撤销抵扣。 @author Punjab */
@Service @RequiredArgsConstructor
public class CustomerBalanceServiceImpl implements CustomerBalanceService {
 private final com.punjab.oms.ar.domain.convert.finance.CustomerBalanceConverter converter;
 private final CashGroupOwnership ownership;
 private final CustomerBalanceDao dao;private final ReceivableDao receivables;private final FinanceJournalService journal;private final FinanceValues values;private final Validator validator;
 /** 按授权主体读取信用账户，将当前余额及版本转换为查询响应。 */
 public com.punjab.oms.ar.domain.vo.finance.CustomerAccountResponse detail(com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery query){validate(query);CurrentUser.require(query.legalEntityId(),"ar:read");var account=dao.detail(converter.toDetailQueryBO(query));if(account==null)throw new ServiceException("客户账户不存在或不可访问");return converter.toResponse(account);}
 /** 查询指定主体下来源批次的金额、占用及版本。 */
 public com.punjab.oms.ar.domain.vo.finance.BalanceLotResponse lotDetail(com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery query){validate(query);CurrentUser.require(query.legalEntityId(),"ar:read");var lot=dao.lotDetail(converter.toDetailQueryBO(query));if(lot==null)throw new ServiceException("余额来源批次不存在或不可访问");return converter.toResponse(lot);}
 /** 校验负数行版本及可转余额，创建来源批次并同步账户与应收。 */
 @Override @Transactional(rollbackFor=Exception.class)
 public Long transfer(BalanceTransferRequest request){
  validate(request);var ticket=journal.begin(new JournalBeginRequest(request.legalEntityId(),"ar:balance:transfer","BALANCE_TRANSFER",request.requestKey(),values.json(request)));
  if(ticket.resultReference()!=null)return ticket.resultReference();
  var line=receivables.line(request.negativeLineId(),false);checkLine(line,request.legalEntityId());values.positiveMoney(line.getCurrency(),request.amount());
  var seed=values.initialize(new ArCustomerAccount(),ticket.actor());seed.setCustomerId(line.getCustomerId());seed.setLegalEntityId(line.getLegalEntityId());seed.setCurrency(line.getCurrency());
  var origins=ownership.groupAccounts(line.getLegalEntityId(),line.getCustomerId(),line.getCurrency(),List.of(line.getGroupId()));
  if (!origins.isEmpty()) seed.setCashAccountId(origins.get(line.getGroupId()).getId());
  if(seed.getCashAccountId()!=null) ownership.lockOpen(seed.getCashAccountId(),request.legalEntityId());
  var account=dao.reserve(seed);if(account==null)throw new ServiceException("客户账户不可用");
  receivables.group(line.getGroupId());line=receivables.line(line.getId(),true);checkLine(line,request.legalEntityId());
  if(!line.getVersion().equals(request.expectedLineVersion()) || line.getOpenAmount().negate().compareTo(request.amount())<0)throw new ServiceException("应收版本已变化或可转入余额不足");
  var lot=values.initialize(new ArBalanceLot(),ticket.actor());lot.setAccountId(account.getId());lot.setSourceLineId(line.getId());lot.setCustomerId(line.getCustomerId());lot.setLegalEntityId(line.getLegalEntityId());lot.setCurrency(line.getCurrency());lot.setAmount(request.amount());lot.setUsedAmount(BigDecimal.ZERO);lot.setStatus("ACTIVE");lot.setVersion(0L);check(dao.createLot(lot));
  var changes=new ArrayList<JournalEventRequest.Change>();
  if(account.getVersion()==0L)changes.add(new JournalEventRequest.Change("CUSTOMER_ACCOUNT",account.getId(),null,values.json(account),values.json(account),null,0L));
  changes.add(new JournalEventRequest.Change("BALANCE_LOT",lot.getId(),null,values.json(lot),values.json(lot),null,0L));
  changes.add(moveLine(line,request.amount(),ticket.actor()));changes.add(moveAccount(account,request.amount(),ticket.actor()));
  journal.append(new JournalEventRequest("BALANCE_TRANSFERRED",ticket.actor(),request.reason(),"BALANCE_LOT",lot.getId().toString(),"balance-transfer:"+ticket.id(),null,changes),ticket);journal.complete(ticket,lot.getId());return lot.getId();
 }
 /** 检查批次尚未被占用，扣回信用余额并恢复原负数应收。 */
 @Override @Transactional(rollbackFor=Exception.class)
 public Long reverseTransfer(BalanceTransferReverseRequest request){
  validate(request);var ticket=journal.begin(new JournalBeginRequest(request.legalEntityId(),"ar:reverse","BALANCE_TRANSFER_REVERSE",request.requestKey(),values.json(request)));
  if(ticket.resultReference()!=null)return ticket.resultReference();
  var lot=dao.lot(request.lotId(),request.legalEntityId(),false);if(lot==null)throw new ServiceException("余额来源批次不存在");
  if("REVERSED".equals(lot.getStatus())){journal.complete(ticket,lot.getId());return lot.getId();}
  var account=CreditReversalLocks.lockAccount(dao,ownership,lot.getAccountId(),request.legalEntityId());
  lot=dao.lot(lot.getId(),request.legalEntityId(),true);
  if(lot==null || !account.getId().equals(lot.getAccountId()))throw new ServiceException("批次归属已变化，请重试");
  if("REVERSED".equals(lot.getStatus())){journal.complete(ticket,lot.getId());return lot.getId();}

  if(lot.getUsedAmount().signum()!=0)throw new ServiceException("该批次已被抵扣，请先撤销对应余额核销");
  var line=receivables.line(lot.getSourceLineId(),false);checkLine(line,request.legalEntityId());receivables.group(line.getGroupId());line=receivables.line(line.getId(),true);checkLine(line,request.legalEntityId());
  var changes=new ArrayList<JournalEventRequest.Change>();changes.add(moveLine(line,lot.getAmount().negate(),ticket.actor()));changes.add(moveAccount(account,lot.getAmount().negate(),ticket.actor()));
  String before=values.json(lot);long version=lot.getVersion();lot.setStatus("REVERSED");lot.setVersion(version+1);touch(lot,ticket.actor());check(dao.updateLot(lot,version));
  if(account.getVersion()==0L)changes.add(new JournalEventRequest.Change("CUSTOMER_ACCOUNT",account.getId(),null,values.json(account),values.json(account),null,0L));
  changes.add(new JournalEventRequest.Change("BALANCE_LOT",lot.getId(),before,values.json(Map.of("status","REVERSED")),values.json(lot),version,lot.getVersion()));
  Long original=dao.transferEvent(lot.getId());if(original==null)throw new ServiceException("原转入事件不存在");
  journal.append(new JournalEventRequest("BALANCE_TRANSFER_REVERSED",ticket.actor(),request.reason(),"BALANCE_LOT",lot.getId().toString(),"balance-reverse:"+ticket.id(),original,changes),ticket);journal.complete(ticket,lot.getId());return lot.getId();
 }
 /** 更新应收行金额和版本，校验方向及边界后返回审计变化。 */
 private JournalEventRequest.Change moveLine(ArLine line,BigDecimal amount,String actor){
  String before=values.json(line);long version=line.getVersion();var transferred=line.getTransferredAmount().subtract(amount);var open=line.getOpenAmount().add(amount);
  if(transferred.signum()>0 || open.signum()>0)throw new ServiceException("负数应收转入金额不一致");
  line.setTransferredAmount(transferred);line.setOpenAmount(open);line.setVersion(version+1);touch(line,actor);check(dao.updateTransferredLine(line,version));
  return new JournalEventRequest.Change("LINE",line.getId(),before,values.json(Map.of("transferredAmount",amount.negate(),"openAmount",amount)),values.json(line),version,line.getVersion());
 }
 /** 校验信用余额不会变负，按版本更新账户并记录金额增量。 */
 private JournalEventRequest.Change moveAccount(ArCustomerAccount account,BigDecimal amount,String actor){
  String before=values.json(account);long version=account.getVersion();var balance=account.getBalanceAmount().add(amount);if(balance.signum()<0)throw new ServiceException("客户信用余额不足");account.setBalanceAmount(balance);account.setVersion(version+1);touch(account,actor);check(dao.updateAccount(account,version));
  return new JournalEventRequest.Change("CUSTOMER_ACCOUNT",account.getId(),before,values.json(Map.of("balanceAmount",amount)),values.json(account),version,account.getVersion());
 }
 /** 确认来源行是本主体有效的负数应收。 */
 private void checkLine(ArLine line,Long scope){if(line==null || !scope.equals(line.getLegalEntityId()) || !"ACTIVE".equals(line.getStatus()) || line.getReceivableAmount().signum()>=0)throw new ServiceException("可转入的负数应收不存在");}
 /** 更新变更记录的操作人与时间。 */
 private void touch(ArBaseEntity value,String actor){value.setUpdatedBy(actor);value.setUpdatedTime(LocalDateTime.now());}
 /** 验证请求对象及字段约束，拒绝无效输入。 */
 private void validate(Object request){if(request==null || !validator.validate(request).isEmpty())throw new ServiceException("余额转入请求参数无效");}
 /** 持久化或版本条件更新未命中时抛出异常，中止当前业务。 */
 private void check(boolean ok){if(!ok)throw new ServiceException("余额已变化，请重试");}
}
