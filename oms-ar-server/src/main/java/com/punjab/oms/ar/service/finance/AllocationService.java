package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.dto.finance.AllocationPreviewRequest;
import com.punjab.oms.ar.domain.vo.finance.AllocationPreviewResponse;
/** 核销分配服务。 @author Punjab */
public interface AllocationService {
    AllocationPreviewResponse preview(AllocationPreviewRequest request);
}
