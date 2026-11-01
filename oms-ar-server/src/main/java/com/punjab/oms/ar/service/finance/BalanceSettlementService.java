package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.vo.finance.BalanceSettlementPreviewResponse;
/** 信用账户抵扣应收。 @author Punjab */
public interface BalanceSettlementService {
 BalanceSettlementPreviewResponse preview(BalanceSettlementPreviewRequest request);
 Long confirm(BalanceSettlementConfirmRequest request);
 Long reverse(SettlementReverseRequest request);
}
