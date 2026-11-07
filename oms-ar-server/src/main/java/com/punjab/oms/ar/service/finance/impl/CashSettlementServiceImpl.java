package com.punjab.oms.ar.service.finance.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.punjab.oms.ar.base.*;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.dao.finance.*;
import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.entity.finance.ArLine;
import com.punjab.oms.ar.domain.vo.finance.CashSettlementView;
import com.punjab.oms.ar.enums.AmountType;
import com.punjab.oms.ar.service.finance.*;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.math.BigDecimal;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** 账户入口按充值顺序选择资金，复用逐收款核销与原分配恢复逻辑。 @author Punjab */
@Service @RequiredArgsConstructor
public class CashSettlementServiceImpl implements CashSettlementService {
    private final CashAccountDao cash;
    private final SettlementDao settlements;
    private final AllocationService allocator;
    private final SettlementService direct;
    private final FinanceJournalService journal;
    private final FinanceValues values;
    private final Validator validator;
    private final ObjectMapper json;

    /** 一致快照读取来源和目标，预览不占用金额。 */
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public CashSettlementView preview(BalanceSettlementPreviewRequest request) {
        validate(request);
        CurrentUser.require(request.legalEntityId(),"ar:settle");
        return calculate(request,false);
    }

    /** 锁定账户、来源和组，确认同一预览后在一个事务内逐批过账。 */
    @Transactional(rollbackFor=Exception.class,isolation=Isolation.READ_COMMITTED)
    public CashSettlementView.Posted confirm(BalanceSettlementConfirmRequest request) {
        validate(request);
        var input=request.preview();
        var ticket=journal.begin(new JournalBeginRequest(input.legalEntityId(),"ar:settle","CASH_SETTLE",
                request.requestKey(),values.json(request)));
        if (ticket.resultReference()!=null) {
            var stored=cash.batchSnapshot(ticket.resultReference(),ticket.id());
            try {
                return new CashSettlementView.Posted(ticket.resultReference(),json.readValue(stored,new TypeReference<List<Long>>() {}));
            } catch (Exception e) { throw new ServiceException("现金核销批次结果无法读取"); }
        }
        var view=calculate(input,true);
        if (!view.fingerprint().equals(request.expectedFingerprint()) || view.usedAmount().signum()==0)
            throw new ServiceException("现金或应收已变化，请重新预览");
        var ids=new ArrayList<Long>();
        for (var part:view.funding()) {
            var child=new SettlementPreviewRequest(input.legalEntityId(),part.receiptId(),part.amount(),input.groupIds(),input.strategy());
            var allocation=direct.preview(child);
            if (allocation.usedAmount().compareTo(part.amount())!=0) throw new ServiceException("应收分配发生变化");
            ids.add(direct.confirm(new SettlementConfirmRequest("cash-batch-"+ticket.id()+"-"+part.lotId(),
                    child,allocation.fingerprint(),request.reason())));
        }
        String snapshot=values.json(ids);
        var change=new JournalEventRequest.Change("CASH_SETTLEMENT_BATCH",ticket.id(),null,snapshot,snapshot,null,0L);
        long event=journal.append(new JournalEventRequest("CASH_ACCOUNT_SETTLED",ticket.actor(),request.reason(),
                "CASH_ACCOUNT",input.accountId().toString(),"cash-settle:"+ticket.id(),null,List.of(change)),ticket);
        journal.complete(ticket,event);
        return new CashSettlementView.Posted(event,List.copyOf(ids));
    }

    /** 同时验证现金批次守恒和业务组归属，再把实际可核销金额按FIFO拆分。 */
    private CashSettlementView calculate(BalanceSettlementPreviewRequest input, boolean lock) {
        var account=cash.account(input.accountId(),input.legalEntityId(),lock);
        if (account==null || !"ACTIVE".equals(account.getStatus())) throw new ServiceException("现金账户不可用");
        values.positiveMoney(account.getCurrency(),input.amount());
        if (input.amount().compareTo(account.getAvailableAmount())>0) throw new ServiceException("账户可用现金不足");
        var candidates=new ArrayList<CashSettlementView.Funding>();
        var versions=new ArrayList<Long>();
        BigDecimal total=BigDecimal.ZERO;
        for (var lot:cash.fifo(account.getId())) {
            var receipt=settlements.receipt(lot.getReceiptId(),input.legalEntityId(),lock);
            if(receipt==null) throw new ServiceException("充值批次缺少原收款记录");
            var free=receipt.getAmount().subtract(receipt.getAllocatedAmount()).subtract(lot.getFrozenAmount()).subtract(lot.getWithdrawnAmount());
            if (free.signum()<0) throw new ServiceException("充值批次余额异常");
            if (free.signum()==0) continue;
            total=total.add(free);
            versions.add(receipt.getVersion());
            candidates.add(new CashSettlementView.Funding(lot.getId(),receipt.getId(),free));
        }
        if (total.compareTo(account.getAvailableAmount())!=0) throw new ServiceException("现金账户与充值批次不平");
        var groups=settlements.groups(input.groupIds(),input.legalEntityId(),lock);
        if (groups.size()!=input.groupIds().size()) throw new ServiceException("业务组不存在或重复");
        for (var group:groups)
            if (!account.getId().equals(cash.boundAccount(group.getId()))) throw new ServiceException("禁止跨现金账户核销");
        var rows=settlements.groupLines(input.groupIds(),lock);
        var grouped=new HashMap<Long,List<AllocationPreviewRequest.Line>>();
        for (ArLine row:rows) grouped.computeIfAbsent(row.getGroupId(),id->new ArrayList<>()).add(
                new AllocationPreviewRequest.Line(row.getId(),AmountType.valueOf(row.getAmountType()),row.getOpenAmount()));
        var targets=input.groupIds().stream().map(id->new AllocationPreviewRequest.Group(id,grouped.getOrDefault(id,List.of()))).toList();
        var result=allocator.preview(new AllocationPreviewRequest(input.amount(),Currency.getInstance(account.getCurrency()).getDefaultFractionDigits(),
                input.strategy(),targets));
        var used=input.amount().subtract(result.unallocatedAmount());
        var remaining=used;
        var funding=new ArrayList<CashSettlementView.Funding>();
        for(var lot:candidates) {
            if(remaining.signum()==0) break;
            var take=remaining.min(lot.amount());
            funding.add(new CashSettlementView.Funding(lot.lotId(),lot.receiptId(),take));
            remaining=remaining.subtract(take);
        }
        String serialized=values.json(List.of(input,account.getVersion(),versions,candidates,rows));
        return new CashSettlementView(hash(serialized),used,result.unallocatedAmount(),result.allocations(),List.copyOf(funding));
    }

    /** 用摘要检测预览后账户或批次发生的变化。 */
    private String hash(String input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    /** 校验必填字段、金额和请求长度。 */
    private void validate(Object input) {
        if(input==null || !validator.validate(input).isEmpty()) throw new ServiceException("现金核销请求无效");
    }
}
