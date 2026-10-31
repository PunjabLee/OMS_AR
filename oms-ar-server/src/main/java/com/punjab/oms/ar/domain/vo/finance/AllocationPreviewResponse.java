package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;
import java.util.List;
import com.punjab.oms.ar.enums.AmountType;
/** 分配预览，不占用资金。 @author Punjab */
public record AllocationPreviewResponse(List<Allocation> allocations, BigDecimal unallocatedAmount) {
    public AllocationPreviewResponse { allocations = List.copyOf(allocations); }
    /** 仅记录正数实际分配。 @author Punjab */
    public record Allocation(Long groupId, Long lineId, AmountType amountType, BigDecimal amount) {}
}
