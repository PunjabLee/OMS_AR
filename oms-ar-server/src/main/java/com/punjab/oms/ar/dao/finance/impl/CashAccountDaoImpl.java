package com.punjab.oms.ar.dao.finance.impl;

import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.punjab.oms.ar.dao.finance.CashAccountDao;
import com.punjab.oms.ar.domain.entity.finance.ArCashAccount;
import com.punjab.oms.ar.mapper.finance.ArCashAccountMapper;
import org.springframework.stereotype.Repository;
import com.punjab.oms.ar.domain.entity.finance.ArCashGroupBinding;

/** 现金账户主体归属过滤。 @author Punjab */
@lombok.RequiredArgsConstructor
@Repository
public class CashAccountDaoImpl extends CrudRepository<ArCashAccountMapper,ArCashAccount> implements CashAccountDao {
    private final com.punjab.oms.ar.mapper.finance.ArCashGroupBindingMapper bindings;

    public ArCashAccount byMode(Long customerId, Long legalEntityId, String currency, String invoiceMode) {
        return lambdaQuery().eq(ArCashAccount::getCustomerId, customerId)
                .eq(ArCashAccount::getLegalEntityId, legalEntityId).eq(ArCashAccount::getCurrency, currency)
                .eq(ArCashAccount::getInvoiceMode, invoiceMode).eq(ArCashAccount::getDeleteStatus, false).one();
    }

    public Long boundAccount(Long groupId) {
        var row = bindings.selectOne(com.baomidou.mybatisplus.core.toolkit.Wrappers
                .<ArCashGroupBinding>lambdaQuery().eq(ArCashGroupBinding::getGroupId, groupId)
                .eq(ArCashGroupBinding::getDeleteStatus, false));
        return row == null ? null : row.getCashAccountId();
    }

    public boolean bind(ArCashGroupBinding binding) { return bindings.insert(binding) == 1; }

    public boolean create(ArCashAccount row) { return save(row); }
    public ArCashAccount find(Long id, Long legalEntityId) {
        return lambdaQuery().eq(ArCashAccount::getId,id).eq(ArCashAccount::getLegalEntityId,legalEntityId)
                .eq(ArCashAccount::getDeleteStatus,false).one();
    }
}
