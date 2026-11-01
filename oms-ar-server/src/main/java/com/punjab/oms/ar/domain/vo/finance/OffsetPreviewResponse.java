package com.punjab.oms.ar.domain.vo.finance;
import java.util.List;import java.math.BigDecimal;
/** 剩余负数金额供用户选择后续处理，不自动跨范围抵销。 @author Punjab */
public record OffsetPreviewResponse(boolean linked,Long originalLineId,Long negativeVersion,List<AllocationPreviewResponse.Allocation> allocations,
 BigDecimal usedAmount,BigDecimal unallocatedAmount,BigDecimal remainingCredit,String fingerprint){public OffsetPreviewResponse{allocations=List.copyOf(allocations);}}
