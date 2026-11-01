package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.vo.finance.SettlementPreviewResponse;
/** 核销事务服务。 @author Punjab */
public interface SettlementService {
 SettlementPreviewResponse preview(SettlementPreviewRequest request);
 Long confirm(SettlementConfirmRequest request);
 Long reverse(SettlementReverseRequest request);
}
