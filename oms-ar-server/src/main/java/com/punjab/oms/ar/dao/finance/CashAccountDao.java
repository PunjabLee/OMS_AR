package com.punjab.oms.ar.dao.finance;

import com.punjab.oms.ar.domain.entity.finance.ArCashAccount;

/** 现金账户持久化。 @author Punjab */
public interface CashAccountDao {
    boolean create(ArCashAccount row);
    ArCashAccount find(Long id, Long legalEntityId);
}
