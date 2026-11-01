package com.punjab.oms.ar.domain.vo.finance;
import java.util.*;import java.math.BigDecimal;
/** 预览不占款，确认时必须重新匹配指纹。 @author Punjab */
public record SettlementPreviewResponse(Long receiptVersion,Map<Long,Long> lineVersions,List<AllocationPreviewResponse.Allocation> allocations,BigDecimal usedAmount,BigDecimal unallocatedAmount,String fingerprint){
 public SettlementPreviewResponse{lineVersions=Map.copyOf(lineVersions);allocations=List.copyOf(allocations);}
}
