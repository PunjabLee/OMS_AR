package com.punjab.oms.ar.service.finance.impl;

import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.dao.finance.WithdrawalDao;
import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.dto.finance.WithdrawalRequests.*;
import com.punjab.oms.ar.domain.vo.finance.*;
import com.punjab.oms.ar.service.finance.*;
import java.util.List;
import java.util.Map;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** 受托收款档案与线下提现审批、认领、结果登记。 @author Punjab */
@Service @RequiredArgsConstructor
@Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
public class WithdrawalServiceImpl implements WithdrawalService {
    private final WithdrawalDao dao;
    private final CashHoldService holds;
    private final FinanceJournalService journal;
    private final FinanceValues values;
    private final Validator validator;

    public Long bind(Bind r) {
        validate(r);
        var t=begin(r.legalEntityId(),"payout:manage","PAYOUT_BIND",r.requestKey(),r);
        if(t.resultReference()!=null) return t.resultReference();
        try { java.util.Currency.getInstance(r.currency()); }
        catch(IllegalArgumentException e) { throw new ServiceException("币种无效"); }
        var id=dao.bind(r.legalEntityId(),r.customerId(),r.currency(),r.beneficiaryRef(),r.lastFour(),t.actor());
        return complete(t,"PAYOUT_BOUND","PAYOUT_ACCOUNT",id,r.reason(),null,dao.payout(id,r.legalEntityId()),null,0L);
    }

    public Long disable(Action r) {
        validate(r);
        var t=begin(r.legalEntityId(),"payout:manage","PAYOUT_DISABLE",r.requestKey(),r);
        if(t.resultReference()!=null) return t.resultReference();
        var p=dao.payout(r.id(),r.legalEntityId());
        require(p!=null && "ACTIVE".equals(p.status()),"收款档案不存在或已停用");
        dao.disable(r.id(),t.actor());
        return complete(t,"PAYOUT_DISABLED","PAYOUT_ACCOUNT",r.id(),r.reason(),p,dao.payout(r.id(),r.legalEntityId()),0L,1L);
    }

    public Long apply(Apply r) {
        validate(r);
        var t=begin(r.legalEntityId(),"withdraw:apply","WITHDRAW_APPLY",r.requestKey(),r);
        if(t.resultReference()!=null) return t.resultReference();
        var account=dao.account(r.cashAccountId(),r.legalEntityId());
        require(account!=null && "ACTIVE".equals(account.status()),"现金账户不可提现");
        var payout=dao.payout(r.payoutAccountId(),r.legalEntityId());
        require(payout!=null && "ACTIVE".equals(payout.status()) && account.customerId()==payout.customerId()
                && account.currency().equals(payout.currency()),"收款档案范围不符或已停用");
        values.positiveMoney(account.currency(),r.amount());
        var hold=holds.reserveWithdrawal(r.legalEntityId(),account.id(),r.amount(),t,r.reason());
        var id=dao.apply(r.legalEntityId(),account,payout.id(),hold,r.amount(),t.actor());
        return complete(t,"WITHDRAW_APPLIED","WITHDRAWAL",id,r.reason(),null,dao.detail(r.legalEntityId(),id),null,0L);
    }

    public Long approve(Action r) { return transition(r,"APPROVED","withdraw:review"); }
    public Long reject(Action r) { return transition(r,"REJECTED","withdraw:review"); }
    public Long cancel(Action r) { return transition(r,"CANCELLED","withdraw:apply"); }
    public Long claim(Action r) { return transition(r,"PROCESSING","withdraw:pay"); }

    private Long transition(Action r,String next,String perm) {
        validate(r);
        var t=begin(r.legalEntityId(),perm,"WITHDRAW_"+next,r.requestKey(),r);
        if(t.resultReference()!=null) return t.resultReference();
        var row=lock(r.legalEntityId(),r.id());
        var before=dao.detail(r.legalEntityId(),r.id());
        switch(next) {
            case "APPROVED","REJECTED" -> require("PENDING".equals(row.status()) && !row.createdBy().equals(t.actor()),"仅独立审核人可审核待审批提现");
            case "CANCELLED" -> require(List.of("PENDING","APPROVED").contains(row.status()) && row.createdBy().equals(t.actor()),"仅申请人可撤销未认领提现");
            case "PROCESSING" -> {
                require("APPROVED".equals(row.status()),"提现须先获批才能认领付款");
                var a=dao.account(row.cashAccountId(),r.legalEntityId());
                var p=dao.payout(row.payoutAccountId(),r.legalEntityId());
                require(a!=null && p!=null && "ACTIVE".equals(a.status()) && "ACTIVE".equals(p.status()),"已停用账户禁止新认领");
            }
            default -> throw new IllegalStateException(next);
        }
        if(next.equals("REJECTED") || next.equals("CANCELLED"))
            holds.releaseWithdrawal(r.legalEntityId(),row.cashAccountId(),row.holdId(),t,r.reason());
        dao.change(row.id(),next,t.actor(),null,null);
        return complete(t,"WITHDRAW_"+next,"WITHDRAWAL",row.id(),r.reason(),before,dao.detail(r.legalEntityId(),row.id()),row.version(),row.version()+1);
    }

    public Long result(Result r) {
        validate(r);
        var t=begin(r.legalEntityId(),"withdraw:pay","WITHDRAW_RESULT",r.requestKey(),r);
        if(t.resultReference()!=null) return t.resultReference();
        var row=lock(r.legalEntityId(),r.id());
        var before=dao.detail(r.legalEntityId(),r.id());
        require(List.of("PROCESSING","UNKNOWN").contains(row.status()) && t.actor().equals(before.claimedBy()),"须由认领人登记在途付款结果");
        if(r.outcome()==Outcome.PAID) {
            require(r.bankReceipt()!=null && !r.bankReceipt().isBlank(),"付款成功须提供银行回单号");
            holds.captureWithdrawal(r.legalEntityId(),row.cashAccountId(),row.holdId(),t,r.reason());
        } else {
            require(r.bankReceipt()==null,"仅付款成功可登记银行回单号");
            if(r.outcome()==Outcome.FAILED) {
                require(r.evidence()!=null && !r.evidence().isBlank(),"确认失败须提供银行失败凭证");
                holds.releaseWithdrawal(r.legalEntityId(),row.cashAccountId(),row.holdId(),t,r.reason());
            }
        }
        dao.change(row.id(),r.outcome().name(),t.actor(),r.bankReceipt()==null?null:r.bankReceipt().strip(),r.evidence());
        return complete(t,"WITHDRAW_"+r.outcome(),"WITHDRAWAL",row.id(),r.reason(),before,dao.detail(r.legalEntityId(),row.id()),row.version(),row.version()+1);
    }

    @Transactional(readOnly=true)
    public WithdrawalResponse detail(Long scope,Long id) {
        CurrentUser.require(scope,"ar:withdraw:read");
        var response=dao.detail(scope,id);
        require(response!=null,"提现不存在或不可访问");
        return response;
    }
    private WithdrawalDao.Row lock(long scope,long id) {
        var row=dao.lock(scope,id);
        require(row!=null,"提现不存在或不可访问");
        return row;
    }
    private JournalTicket begin(Long scope,String perm,String action,String key,Object r) {
        return journal.begin(new JournalBeginRequest(scope,"ar:"+perm,action,key,values.json(r)));
    }
    private Long complete(JournalTicket t,String event,String type,long id,String reason,Object before,Object after,Long oldVersion,long newVersion) {
        String snapshot=values.json(after);
        var change=new JournalEventRequest.Change(type,id,before==null?null:values.json(before),
                values.json(Map.of("operation",event)),snapshot,oldVersion,newVersion);
        journal.append(new JournalEventRequest(event,t.actor(),reason,type,Long.toString(id),
                "withdrawal:"+t.id(),null,List.of(change)),t);
        journal.complete(t,id);
        return id;
    }
    private void validate(Object r) { require(r!=null && validator.validate(r).isEmpty(),"提现参数无效"); }
    private static void require(boolean ok,String msg) { if(!ok) throw new ServiceException(msg); }
}
