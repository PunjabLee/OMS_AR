package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;import java.util.List;
/** 同时显示应收分配与FIFO来源批次，预览不占款。 @author Punjab */
public record BalanceSettlementPreviewResponse(Long accountVersion,List<AllocationPreviewResponse.Allocation> allocations,List<LotAllocation> lots,BigDecimal usedAmount,BigDecimal unallocatedAmount,String fingerprint){
 public BalanceSettlementPreviewResponse{allocations=List.copyOf(allocations);lots=List.copyOf(lots);}
 /** 一条应收分配可能使用多个来源批次。 @author Punjab */
 public record LotAllocation(Long lineId,Long lotId,BigDecimal amount){}
}
