package com.punjab.oms.ar.service.finance.impl;
import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.dao.finance.CashHoldDao;
import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.dto.finance.CashHoldRequest.*;
import com.punjab.oms.ar.domain.dto.finance.JournalEventRequest.Change;
import com.punjab.oms.ar.domain.entity.finance.*;
import com.punjab.oms.ar.domain.vo.finance.*;
import com.punjab.oms.ar.service.finance.*;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static java.math.BigDecimal.ZERO;

/** 冻结按充值顺序占款，解冻与支付只处理该冻结单原先占用的批次。 */
@Service @RequiredArgsConstructor
public class CashHoldServiceImpl implements CashHoldService {
 private final CashHoldDao dao;
 private final FinanceJournalService journal;
 private final FinanceValues values;
 private final Validator validator;

 @Transactional(rollbackFor=Exception.class)
 public Long freeze(Freeze r){
  validate(r);
  var ticket=journal.begin(new JournalBeginRequest(r.legalEntityId(),"ar:cash:freeze","CASH_FREEZE",r.requestKey(),values.json(r)));
  if(ticket.resultReference()!=null)return ticket.resultReference();
  Long id=reserve(r.legalEntityId(),r.accountId(),r.amount(),"MANUAL",ticket,r.reason());
  journal.complete(ticket,id);
  return id;
 }
 @Transactional(rollbackFor=Exception.class)
 public Long unfreeze(Release r){
  validate(r);
  var ticket=journal.begin(new JournalBeginRequest(r.legalEntityId(),"ar:cash:unfreeze","CASH_UNFREEZE",r.requestKey(),values.json(r)));
  if(ticket.resultReference()!=null)return ticket.resultReference();
  finish(r.legalEntityId(),r.accountId(),r.holdId(),r.amount(),"MANUAL",false,ticket,r.reason());
  journal.complete(ticket,r.holdId());
  return r.holdId();
 }
 @Transactional(propagation=Propagation.MANDATORY)
 public Long reserveWithdrawal(Long scope,Long accountId,BigDecimal amount,JournalTicket ticket,String reason){
  return reserve(scope,accountId,amount,"WITHDRAWAL",ticket,reason);
 }
 @Transactional(propagation=Propagation.MANDATORY)
 public void captureWithdrawal(Long scope,Long accountId,Long holdId,JournalTicket ticket,String reason){
  finish(scope,accountId,holdId,null,"WITHDRAWAL",true,ticket,reason);
 }
 @Transactional(propagation=Propagation.MANDATORY)
 public void releaseWithdrawal(Long scope,Long accountId,Long holdId,JournalTicket ticket,String reason){
  finish(scope,accountId,holdId,null,"WITHDRAWAL",false,ticket,reason);
 }
 private Long reserve(Long scope,Long accountId,BigDecimal amount,String purpose,JournalTicket ticket,String reason){
  var account=lockedAccount(scope,accountId,ticket);
  values.positiveMoney(account.getCurrency(),amount);
  if(!"ACTIVE".equals(account.getStatus()) || account.getAvailableAmount().compareTo(amount)<0)
   throw new ServiceException("账户不可新增冻结或可用现金不足");
  var changes=new ArrayList<Change>();
  String actor=ticket.actor();
  var hold=values.initialize(new ArCashHold(),actor);
  hold.setCashAccountId(accountId);hold.setPurpose(purpose);hold.setAmount(amount);
  hold.setReleasedAmount(ZERO);hold.setCapturedAmount(ZERO);hold.setVersion(0L);
  dao.insert(hold);changes.add(initial("CASH_HOLD",hold));
  BigDecimal remaining=amount;
  for(var lot:dao.lots(accountId)){
   var receipt=dao.receipt(lot.getReceiptId());
   if(receipt==null)throw new ServiceException("冻结来源收款缺失");
   var free=receipt.getAmount().subtract(receipt.getAllocatedAmount()).subtract(lot.getWithdrawnAmount()).subtract(lot.getFrozenAmount());
   if(free.signum()<0)throw new ServiceException("收款批次可用金额异常");
   if(free.signum()==0)continue;
   var part=remaining.min(free);
   changeLot(lot,part,ZERO,actor,changes);
   var slice=values.initialize(new ArCashHoldSlice(),actor);
   slice.setCashAccountId(accountId);slice.setHoldId(hold.getId());slice.setLotId(lot.getId());
   slice.setAmount(part);slice.setReleasedAmount(ZERO);slice.setCapturedAmount(ZERO);
   dao.insert(slice);changes.add(initial("CASH_HOLD_SLICE",slice));
   remaining=remaining.subtract(part);
   if(remaining.signum()==0)break;
  }
  if(remaining.signum()!=0)throw new ServiceException("充值批次可用余额不足");
  changeAccount(account,amount,ZERO,actor,changes);
  append("CASH_HOLD_PLACED",hold.getId(),reason,ticket,changes);
  return hold.getId();
 }
 private void finish(Long scope,Long accountId,Long id,BigDecimal requested,String purpose,boolean capture,JournalTicket ticket,String reason){
  var account=lockedAccount(scope,accountId,ticket);
  var hold=dao.hold(id,accountId,true);
  if(hold==null || !purpose.equals(hold.getPurpose()))throw new ServiceException("冻结单不存在或用途不匹配");
  var outstanding=hold.getAmount().subtract(hold.getReleasedAmount()).subtract(hold.getCapturedAmount());
  var amount=requested==null?outstanding:requested;
  values.positiveMoney(account.getCurrency(),amount);
  if(amount.compareTo(outstanding)>0)throw new ServiceException("金额超过冻结单剩余额");
  var changes=new ArrayList<Change>();
  BigDecimal remaining=amount;
  for(var slice:dao.slices(id,true)){
   var part=remaining.min(slice.getAmount().subtract(slice.getCapturedAmount()).subtract(slice.getReleasedAmount()));
   if(part.signum()==0)continue;
   var lot=dao.lot(slice.getLotId(),accountId);
   if(lot==null || lot.getFrozenAmount().compareTo(part)<0)throw new ServiceException("原冻结批次余额异常");
   changeLot(lot,part.negate(),capture?part:ZERO,ticket.actor(),changes);
   String before=values.json(slice);
   if(capture)slice.setCapturedAmount(slice.getCapturedAmount().add(part));
   else slice.setReleasedAmount(slice.getReleasedAmount().add(part));
   touch(slice,ticket.actor());dao.update(slice);
   changes.add(change("CASH_HOLD_SLICE",slice,before,Map.of(capture?"capturedAmount":"releasedAmount",part),0L,0L));
   remaining=remaining.subtract(part);
   if(remaining.signum()==0)break;
  }
  if(remaining.signum()!=0)throw new ServiceException("冻结单明细余额不足");
  String before=values.json(hold);long version=hold.getVersion();
  if(capture)hold.setCapturedAmount(hold.getCapturedAmount().add(amount));
  else hold.setReleasedAmount(hold.getReleasedAmount().add(amount));
  hold.setVersion(version+1);touch(hold,ticket.actor());dao.update(hold);
  changes.add(change("CASH_HOLD",hold,before,Map.of(capture?"capturedAmount":"releasedAmount",amount),version,version+1));
  changeAccount(account,amount.negate(),capture?amount.negate():ZERO,ticket.actor(),changes);
  append(capture?"CASH_HOLD_CAPTURED":"CASH_HOLD_RELEASED",id,reason,ticket,changes);
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public CashHoldView detail(Long scope,Long accountId,Long id){
  CurrentUser.require(scope,"ar:cash:read");
  if(dao.account(accountId,scope,false)==null)throw new ServiceException("账户不存在或不可访问");
  var hold=dao.hold(id,accountId,false);
  if(hold==null)throw new ServiceException("冻结单不存在");
  var slices=dao.slices(id,false).stream().map(s->new CashHoldView.Slice(s.getLotId(),s.getAmount(),s.getReleasedAmount(),s.getCapturedAmount())).toList();
  return new CashHoldView(id,accountId,hold.getPurpose(),hold.getAmount(),hold.getReleasedAmount(),hold.getCapturedAmount(),hold.getAmount().subtract(hold.getReleasedAmount()).subtract(hold.getCapturedAmount()),slices);
 }
 private ArCashAccount lockedAccount(Long scope,Long id,JournalTicket ticket){
  if(!scope.equals(ticket.legalEntityId()))throw new ServiceException("冻结操作主体不一致");
  var account=dao.account(id,scope,true);
  if(account==null)throw new ServiceException("账户不存在或不可访问");
  return account;
 }
 private void changeAccount(ArCashAccount account,BigDecimal frozen,BigDecimal book,String actor,List<Change> changes){
  String before=values.json(account);long version=account.getVersion();
  account.setBookAmount(account.getBookAmount().add(book));
  account.setFrozenAmount(account.getFrozenAmount().add(frozen));
  account.setAvailableAmount(account.getBookAmount().subtract(account.getFrozenAmount()));
  account.setVersion(version+1);touch(account,actor);dao.update(account);
  changes.add(change("CASH_ACCOUNT",account,before,Map.of("bookAmount",book,"frozenAmount",frozen,"availableAmount",book.subtract(frozen)),version,version+1));
 }
 private void changeLot(ArCashReceiptLot lot,BigDecimal frozen,BigDecimal withdrawn,String actor,List<Change> changes){
  String before=values.json(lot);
  lot.setFrozenAmount(lot.getFrozenAmount().add(frozen));lot.setWithdrawnAmount(lot.getWithdrawnAmount().add(withdrawn));
  touch(lot,actor);dao.update(lot);
  changes.add(change("CASH_RECEIPT_LOT",lot,before,Map.of("frozenAmount",frozen,"withdrawnAmount",withdrawn),0L,0L));
 }
 private Change initial(String type,ArBaseEntity row){
  String json=values.json(row);return new Change(type,row.getId(),null,json,json,null,0L);
 }
 private Change change(String type,ArBaseEntity row,String before,Object delta,Long oldVersion,Long newVersion){
  return new Change(type,row.getId(),before,values.json(delta),values.json(row),oldVersion,newVersion);
 }
 private void append(String action,Long id,String reason,JournalTicket ticket,List<Change> changes){
  journal.append(new JournalEventRequest(action,ticket.actor(),reason,"CASH_HOLD",id.toString(),"cash-hold:"+ticket.id(),null,changes),ticket);
 }
 private void touch(ArBaseEntity row,String actor){row.setUpdatedBy(actor);row.setUpdatedTime(LocalDateTime.now());}
 private void validate(Object request){if(request==null || !validator.validate(request).isEmpty())throw new ServiceException("冻结请求无效");}
}
