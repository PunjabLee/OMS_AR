package com.punjab.oms.ar.service.finance;

import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.query.finance.BalanceDetailQuery;
import com.punjab.oms.ar.domain.vo.finance.CashAccountResponse;

/** 现金账户业务入口。 @author Punjab */
public interface CashAccountService {
    Long stop(CashStatusRequest request);
    Long resume(CashStatusRequest request);
    Long open(CashAccountOpenRequest request);
    CashAccountResponse detail(BalanceDetailQuery query);
}
