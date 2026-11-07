package com.punjab.oms.ar.dao.finance.impl;

import com.baomidou.mybatisplus.extension.repository.CrudRepository;
import com.punjab.oms.ar.dao.finance.CashAccountDao;
import com.punjab.oms.ar.domain.entity.finance.ArCashAccount;
import com.punjab.oms.ar.mapper.finance.ArCashAccountMapper;
import org.springframework.stereotype.Repository;

/** 现金账户主体归属过滤。 @author Punjab */
@Repository
public class CashAccountDaoImpl extends CrudRepository<ArCashAccountMapper,ArCashAccount> implements CashAccountDao {
    public boolean create(ArCashAccount row) { return save(row); }
    public ArCashAccount find(Long id, Long legalEntityId) {
        return lambdaQuery().eq(ArCashAccount::getId,id).eq(ArCashAccount::getLegalEntityId,legalEntityId)
                .eq(ArCashAccount::getDeleteStatus,false).one();
    }
}
