package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;
/** 账户、来源批次和转入应收的聚合访问。 @author Punjab */
public interface CustomerBalanceDao {
 ArCustomerAccount detail(com.punjab.oms.ar.domain.bo.finance.BalanceDetailQueryBO query);
 ArBalanceLot lotDetail(com.punjab.oms.ar.domain.bo.finance.BalanceDetailQueryBO query);
 ArCustomerAccount reserve(ArCustomerAccount value);
 ArCustomerAccount account(Long id,Long entity,boolean lock);
 ArBalanceLot lot(Long id,Long entity,boolean lock);
 boolean createLot(ArBalanceLot value);
 boolean updateAccount(ArCustomerAccount value,Long expected);
 boolean updateLot(ArBalanceLot value,Long expected);
 boolean updateTransferredLine(ArLine value,Long expected);
 Long transferEvent(Long lotId);
}
