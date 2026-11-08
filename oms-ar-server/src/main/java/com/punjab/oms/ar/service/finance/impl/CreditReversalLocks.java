package com.punjab.oms.ar.service.finance.impl;

import com.punjab.oms.ar.base.ServiceException;
import com.punjab.oms.ar.dao.finance.CustomerBalanceDao;
import com.punjab.oms.ar.domain.entity.finance.ArCustomerAccount;
import java.util.Objects;

/** 信用撤销按转入相同的现金账户、信用账户顺序取得行锁。 @author Punjab */
final class CreditReversalLocks {
    private CreditReversalLocks() {}

    /** 普通读定位归属，先锁现金账户再锁信用账户，并复核归属。历史未分户账户仅锁信用账户。 */
    static ArCustomerAccount lockAccount(CustomerBalanceDao balances, CashGroupOwnership ownership,
                                         Long accountId, Long legalEntityId) {
        var reference = balances.account(accountId, legalEntityId, false);
        if (reference == null) throw new ServiceException("客户信用账户不存在");
        Long owner = reference.getCashAccountId();
        if (owner != null) ownership.lockOpen(owner, legalEntityId);
        var locked = balances.account(accountId, legalEntityId, true);
        if (locked == null || !Objects.equals(owner, locked.getCashAccountId()))
            throw new ServiceException("客户信用账户归属已变化，请重试");
        return locked;
    }
}
