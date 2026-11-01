package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.dto.finance.*;import com.punjab.oms.ar.domain.vo.finance.OffsetPreviewResponse;
/** 正负应收核销。 @author Punjab */
public interface OffsetService {
 OffsetPreviewResponse preview(OffsetPreviewRequest request);
 Long confirm(OffsetConfirmRequest request);
}
