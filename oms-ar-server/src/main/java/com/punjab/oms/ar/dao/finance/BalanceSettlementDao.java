package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;import java.util.List;
/** 余额核销账户及不可变批次分配记录。 @author Punjab */
public interface BalanceSettlementDao {
 boolean create(ArBalanceSettlement value);boolean createUsage(ArBalanceUsage value);
 ArBalanceSettlement source(Long settlementId);List<ArBalanceUsage> usages(Long settlementId);
 List<ArBalanceLot> availableLots(Long accountId,boolean lock);List<ArBalanceLot> lots(List<Long> ids,boolean lock);
}
