package com.punjab.oms.ar.service.finance.impl;

import com.punjab.oms.ar.base.ServiceException;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.dao.finance.CashAccountDao;
import com.punjab.oms.ar.domain.dto.finance.JournalEventRequest.Change;
import com.punjab.oms.ar.domain.entity.finance.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 在调用方财务事务内维护收款批次和账户余额。 @author Punjab */
@Component @RequiredArgsConstructor
public class CashReceiptPosting {
    private final CashAccountDao dao;
    private final CashGroupOwnership ownership;
    private final FinanceValues values;

    /** 先锁定入款账户，完成批次和账户同事务登记。 */
    public void deposit(ArReceipt receipt, Long id, String actor, List<Change> changes) {
        boolean enabled=ownership.enabled(receipt.getLegalEntityId(),receipt.getCustomerId(),receipt.getCurrency());
        if (!enabled && id==null) return;
        if (!enabled || id==null) throw new ServiceException("收款与当前现金分户范围不匹配");
        var account=dao.account(id,receipt.getLegalEntityId(),true);
        if (account==null || !account.getCustomerId().equals(receipt.getCustomerId())
                || !account.getCurrency().equals(receipt.getCurrency()) || "CLOSED".equals(account.getStatus()))
            throw new ServiceException("充值账户不存在、已关闭或归属不一致");
        var lot=values.initialize(new ArCashReceiptLot(),actor);
        lot.setReceiptId(receipt.getId());
        lot.setCashAccountId(id);
        lot.setCustomerId(receipt.getCustomerId());
        lot.setLegalEntityId(receipt.getLegalEntityId());
        lot.setCurrency(receipt.getCurrency());
        lot.setFrozenAmount(BigDecimal.ZERO);
        lot.setWithdrawnAmount(BigDecimal.ZERO);
        if (!dao.insertLot(lot)) throw new ServiceException("充值批次保存失败");
        String initial=values.json(lot);
        changes.add(new Change("CASH_RECEIPT_LOT",lot.getId(),null,initial,initial,null,0L));
        changes.add(move(account,receipt.getAmount(),actor));
    }

    /** 从收款定位不可变的批次归属，先取得账户锁再由调用方锁收款。 */
    public ArCashAccount account(Long receiptId, Long scope, boolean lock) {
        var lot=dao.receiptLot(receiptId);
        if (lot==null) return null;
        var account=dao.account(lot.getCashAccountId(),scope,lock);
        if (account==null) throw new ServiceException("充值账户不存在或不可访问");
        return account;
    }

    /** 按收款批次及账户可用额双重校验，并拒绝跨户核销。 */
    public void validate(ArReceipt receipt, ArCashAccount account, List<Long> groups, BigDecimal amount) {
        if (account==null) {
            if (ownership.enabled(receipt.getLegalEntityId(),receipt.getCustomerId(),receipt.getCurrency()))
                throw new ServiceException("原收款尚未映射现金账户");
            return;
        }
        if (!"ACTIVE".equals(account.getStatus()) || account.getAvailableAmount().compareTo(amount)<0)
            throw new ServiceException("账户已止付或可用现金不足");
        var lot=dao.receiptLot(receipt.getId());
        if(dao.reversed(lot.getId())) throw new ServiceException("收款已冲正");
        var free=receipt.getAmount().subtract(receipt.getAllocatedAmount()).subtract(lot.getFrozenAmount()).subtract(lot.getWithdrawnAmount());
        if (free.compareTo(amount)<0) throw new ServiceException("原收款批次可用金额不足");
        for (Long group : groups)
            if (!account.getId().equals(dao.boundAccount(group))) throw new ServiceException("禁止跨现金账户核销");
    }

    /** 带版本更新账面额并形成同一财务事件中的账户快照。 */
    public Change move(ArCashAccount account, BigDecimal delta, String actor) {
        if("CLOSED".equals(account.getStatus())) throw new ServiceException("请先恢复现金账户");
        String before=values.json(account);
        long version=account.getVersion();
        var book=account.getBookAmount().add(delta);
        if (book.compareTo(account.getFrozenAmount())<0) throw new ServiceException("现金账户可用余额不足");
        account.setBookAmount(book);
        account.setAvailableAmount(book.subtract(account.getFrozenAmount()));
        account.setUpdatedBy(actor);
        account.setUpdatedTime(LocalDateTime.now());
        account.setVersion(version+1);
        if (!dao.updateBook(account,version)) throw new ServiceException("现金账户余额已变化");
        return new Change("CASH_ACCOUNT",account.getId(),before,values.json(Map.of("bookAmount",delta,"availableAmount",delta)),
                values.json(account),version,version+1);
    }
}
