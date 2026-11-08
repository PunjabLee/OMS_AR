package com.punjab.oms.ar.service.finance;

import com.punjab.oms.ar.domain.dto.finance.WithdrawalRequests.*;
import com.punjab.oms.ar.domain.vo.finance.WithdrawalResponse;

/** 提现业务入口。 @author Punjab */
public interface WithdrawalService {
    Long bind(Bind request);
    Long disable(Action request);
    Long apply(Apply request);
    Long approve(Action request);
    Long reject(Action request);
    Long cancel(Action request);
    Long claim(Action request);
    Long result(Result request);
    WithdrawalResponse detail(Long scope, Long id);
}
