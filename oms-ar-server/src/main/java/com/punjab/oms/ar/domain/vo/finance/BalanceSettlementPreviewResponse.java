package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;import java.util.List;
/** 同时显示应收分配与FIFO来源批次，预览不占款。 @author Punjab */
public record BalanceSettlementPreviewResponse(
    /** 预览时的账户版本 */
    Long accountVersion,
    /** 逐行分配明细 */
    List<AllocationPreviewResponse.Allocation> allocations,
    /** 本次涉及的信用来源批次或分配 */
    List<LotAllocation> lots,
    /** 已占用或本次实际使用的金额 */
    BigDecimal usedAmount,
    /** 本次请求金额中未分配的部分 */
    BigDecimal unallocatedAmount,
    /** 当前预览所依赖数据的摘要 */
    String fingerprint){
 public BalanceSettlementPreviewResponse{allocations=List.copyOf(allocations);lots=List.copyOf(lots);}
 /** 一条应收分配可能使用多个来源批次。 @author Punjab */
 public record LotAllocation(
    /** 业务明细行标识 */
    Long lineId,
    /** 信用余额来源批次ID */
    Long lotId,
    /** 本记录的业务金额，方向由业务类型决定 */
    BigDecimal amount){}
}
