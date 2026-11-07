package com.punjab.oms.ar.service.finance;

import com.punjab.oms.ar.domain.dto.finance.*;
import com.punjab.oms.ar.domain.vo.finance.CashSettlementView;

/** 客户现金账户核销。 @author Punjab */
public interface CashSettlementService {
    CashSettlementView preview(BalanceSettlementPreviewRequest request);
    CashSettlementView.Posted confirm(BalanceSettlementConfirmRequest request);
}
