package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.base.PageResult;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.*;
/** 财务记录分页，不返回Web响应。 @author Punjab */
public interface FinancePageDao {
 PageResult<ArSettlement> settlements(SettlementPageQueryBO query);
 PageResult<ArCustomerAccount> accounts(AccountPageQueryBO query);
 PageResult<ArBalanceLot> lots(LotPageQueryBO query);
}
