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

    /** 以管理权限登记客户及币种对应的受托收款引用和账号尾号。 */
    public Long bind(Bind r) {
        validate(r);
        var t=begin(r.legalEntityId(),"payout:manage","PAYOUT_BIND",r.requestKey(),r);
        if(t.resultReference()!=null) return t.resultReference();
        try { java.util.Currency.getInstance(r.currency()); }
        catch(IllegalArgumentException e) { throw new ServiceException("币种无效"); }
        var id=dao.bind(r.legalEntityId(),r.customerId(),r.currency(),r.beneficiaryRef(),r.lastFour(),t.actor());
        return complete(t,"PAYOUT_BOUND","PAYOUT_ACCOUNT",id,r.reason(),null,dao.payout(id,r.legalEntityId()),null,0L);
    }

    /** 锁定有效收款档案后停用，后续申请及认领会重新检查档案状态。 */
    public Long disable(Action r) {
        validate(r);
        var t=begin(r.legalEntityId(),"payout:manage","PAYOUT_DISABLE",r.requestKey(),r);
        if(t.resultReference()!=null) return t.resultReference();
        var p=dao.payout(r.id(),r.legalEntityId());
        require(p!=null && "ACTIVE".equals(p.status()),"收款档案不存在或已停用");
        dao.disable(r.id(),t.actor());
        return complete(t,"PAYOUT_DISABLED","PAYOUT_ACCOUNT",r.id(),r.reason(),p,dao.payout(r.id(),r.legalEntityId()),0L,1L);
    }

    /** 核对现金账户和收款档案的客户、主体及币种，在申请事务内冻结提现金额。 */
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

    /** 由申请人以外的审核人批准待审申请，资金继续冻结以待付款。 */
    public Long approve(Action r) { return transition(r,"APPROVED","withdraw:review"); }
    /** 由独立审核人拒绝待审申请，同时释放原提现冻结。 */
    public Long reject(Action r) { return transition(r,"REJECTED","withdraw:review"); }
    /** 允许原申请人在付款认领前撤销待审或已批准的申请，并释放冻结。 */
    public Long cancel(Action r) { return transition(r,"CANCELLED","withdraw:apply"); }
    /** 对已批准申请重新核验账户和收款档案，登记本次线下付款的认领人。 */
    public Long claim(Action r) { return transition(r,"PROCESSING","withdraw:pay"); }

    /** 锁定申请后检验当前状态和操作者。拒绝及撤销与释放冻结在同一事务提交。 */
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

    /** 仅认领人可登记在途结果。成功按回单扣款，确认失败凭证解冻，结果未知时保留原冻结。 */
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

    /** 校验主体及提现读取权限后返回带掩码的收款账户信息。 */
    @Transactional(readOnly=true)
    public WithdrawalResponse detail(Long scope,Long id) {
        CurrentUser.require(scope,"ar:withdraw:read");
        var response=dao.detail(scope,id);
        require(response!=null,"提现不存在或不可访问");
        return response;
    }
    /** 按主体锁定提现行，后续状态判断使用锁定后的数据。 */
    private WithdrawalDao.Row lock(long scope,long id) {
        var row=dao.lock(scope,id);
        require(row!=null,"提现不存在或不可访问");
        return row;
    }
    /** 用可信主体权限、动作和请求键预留幂等操作，并保存请求摘要。 */
    private JournalTicket begin(Long scope,String perm,String action,String key,Object r) {
        return journal.begin(new JournalBeginRequest(scope,"ar:"+perm,action,key,values.json(r)));
    }
    /** 追加状态变更快照和业务事件，再保存本次请求可重放的结果标识。 */
    private Long complete(JournalTicket t,String event,String type,long id,String reason,Object before,Object after,Long oldVersion,long newVersion) {
        String snapshot=values.json(after);
        var change=new JournalEventRequest.Change(type,id,before==null?null:values.json(before),
                values.json(Map.of("operation",event)),snapshot,oldVersion,newVersion);
        journal.append(new JournalEventRequest(event,t.actor(),reason,type,Long.toString(id),
                "withdrawal:"+t.id(),null,List.of(change)),t);
        journal.complete(t,id);
        return id;
    }
    /** 进入提现状态处理前校验请求字段及金额范围。 */
    private void validate(Object r) { require(r!=null && validator.validate(r).isEmpty(),"提现参数无效"); }
    /** 业务前置条件不成立时抛出异常，终止本次事务。 */
    private static void require(boolean ok,String msg) { if(!ok) throw new ServiceException(msg); }
}
