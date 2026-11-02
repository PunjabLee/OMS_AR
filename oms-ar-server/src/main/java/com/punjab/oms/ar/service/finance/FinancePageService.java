package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.base.PageResult;import com.punjab.oms.ar.domain.query.finance.*;import com.punjab.oms.ar.domain.vo.finance.*;
/** 财务分页查询边界。 @author Punjab */
public interface FinancePageService {
 PageResult<SettlementResponse.Header> settlements(SettlementPageQuery query);
 PageResult<CustomerAccountResponse> accounts(AccountPageQuery query);
 PageResult<BalanceLotResponse> lots(LotPageQuery query);
}
