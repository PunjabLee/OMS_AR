package com.punjab.oms.ar.service.finance.impl;

import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery;
import com.punjab.oms.ar.domain.vo.finance.CashAccountResponse;
import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.dao.finance.CashAccountDao;
import com.punjab.oms.ar.domain.convert.finance.CashAccountConverter;
import com.punjab.oms.ar.service.finance.*;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;

/** 现金账户开户与授权查询。 @author Punjab */
@Service @RequiredArgsConstructor
public class CashAccountServiceImpl implements CashAccountService {
    private final CashAccountDao accounts;
    private final FinanceJournalService journal;
    private final CashAccountConverter converter;
    private final FinanceValues values;
    private final Validator validator;

    /** 校验开户用途及币种，幂等创建零余额账户并保存初始化快照。 */
    @Transactional(rollbackFor=Exception.class)
    public Long open(CashAccountOpenRequest request) {
        validate(request);
        var ticket = journal.begin(new JournalBeginRequest(request.legalEntityId(),"ar:cash:open",
                "CASH_ACCOUNT_OPEN",request.requestKey(),values.json(request)));
        if (ticket.resultReference()!=null) return ticket.resultReference();
        try {
            int scale=Currency.getInstance(request.currency()).getDefaultFractionDigits();
            if (scale<0 || scale>8) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            throw new ServiceException("现金账户币种无效");
        }
        var row=values.initialize(converter.toEntity(request),ticket.actor());
        row.setStatus("ACTIVE");
        row.setBookAmount(BigDecimal.ZERO);
        row.setFrozenAmount(BigDecimal.ZERO);
        row.setAvailableAmount(BigDecimal.ZERO);
        row.setVersion(0L);
        if (!accounts.create(row)) throw new ServiceException("现金账户创建失败");
        String snapshot=values.json(row);
        var change=new JournalEventRequest.Change("CASH_ACCOUNT",row.getId(),null,snapshot,snapshot,null,0L);
        journal.append(new JournalEventRequest("CASH_ACCOUNT_OPENED",ticket.actor(),request.reason(),
                "CASH_ACCOUNT",row.getId().toString(),"cash-open:"+ticket.id(),null,List.of(change)),ticket);
        journal.complete(ticket,row.getId());
        return row.getId();
    }

    /** 止付后，保留余额和冻结明细，出账入口依据状态拒绝新增核销。 */
    @Transactional(rollbackFor=Exception.class)
    public Long stop(CashStatusRequest request) {
        return transition(request,"ACTIVE","STOPPED","ar:cash:stop","CASH_ACCOUNT_STOPPED");
    }

    /** 解除整户止付时继续保留各批次原冻结金额。 */
    @Transactional(rollbackFor=Exception.class)
    public Long resume(CashStatusRequest request) {
        return transition(request,"STOPPED","ACTIVE","ar:cash:resume","CASH_ACCOUNT_RESUMED");
    }

    /** 锁定账户并复核状态及版本，状态变更与金额快照在同一事务提交。 */
    private Long transition(CashStatusRequest request,String from,String to,String permission,String action) {
        validate(request);
        var ticket=journal.begin(new JournalBeginRequest(request.legalEntityId(),permission,action,request.requestKey(),values.json(request)));
        if(ticket.resultReference()!=null) return ticket.resultReference();
        var row=accounts.account(request.accountId(),request.legalEntityId(),true);
        if(row==null || !from.equals(row.getStatus()) || !request.expectedVersion().equals(row.getVersion()))
            throw new ServiceException("现金账户状态或版本已变化，请刷新");
        String before=values.json(row);
        long previous=row.getVersion();
        row.setStatus(to);
        row.setVersion(previous+1);
        row.setUpdatedBy(ticket.actor());
        row.setUpdatedTime(java.time.LocalDateTime.now());
        if(!accounts.updateStatus(row,previous)) throw new ServiceException("现金账户版本冲突");
        var change=new JournalEventRequest.Change("CASH_ACCOUNT",row.getId(),before,
                values.json(java.util.Map.of("status",to)),values.json(row),previous,row.getVersion());
        journal.append(new JournalEventRequest(action,ticket.actor(),request.reason(),"CASH_ACCOUNT",
                row.getId().toString(),"cash-status:"+ticket.id(),null,List.of(change)),ticket);
        journal.complete(ticket,row.getId());
        return row.getId();
    }

    /** 仅返回授权主体下该账户的现金余额与冻结额。 */
    public CashAccountResponse detail(BalanceDetailQuery query) {
        validate(query);
        CurrentUser.require(query.legalEntityId(),"ar:cash:read");
        var account=accounts.find(query.id(),query.legalEntityId());
        if (account==null) throw new ServiceException("现金账户不存在或不可访问");
        return converter.toResponse(account);
    }

    /** 拒绝缺少范围或不符合字段约束的请求。 */
    private void validate(Object request) {
        if (request==null || !validator.validate(request).isEmpty()) throw new ServiceException("现金账户请求无效");
    }
}
