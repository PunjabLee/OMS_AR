package com.punjab.oms.ar.domain.vo.finance;
import java.util.List;import java.math.BigDecimal;
/** 剩余负数金额供用户选择后续处理，不自动跨范围抵销。 @author Punjab */
public record OffsetPreviewResponse(
    /** 负数行是否关联原业务组 */
    boolean linked,
    /** 调整所指定的原正数应收行ID */
    Long originalLineId,
    /** 负数应收行的预览版本 */
    Long negativeVersion,
    /** 逐行分配明细 */
    List<AllocationPreviewResponse.Allocation> allocations,
 /** 已占用或本次实际使用的金额 */
 BigDecimal usedAmount,
    /** 本次请求金额中未分配的部分 */
    BigDecimal unallocatedAmount,
    /** 本次抵销后负数行剩余余额的绝对值 */
    BigDecimal remainingCredit,
    /** 当前预览所依赖数据的摘要 */
    String fingerprint){public OffsetPreviewResponse{allocations=List.copyOf(allocations);}}
