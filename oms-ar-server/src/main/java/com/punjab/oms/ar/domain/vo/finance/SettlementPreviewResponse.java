package com.punjab.oms.ar.domain.vo.finance;
import java.util.*;import java.math.BigDecimal;
/** 预览不占款，确认时必须重新匹配指纹。 @author Punjab */
public record SettlementPreviewResponse(
    /** 预览时的收款版本 */
    Long receiptVersion,
    /** 候选应收行ID与版本的映射 */
    Map<Long,Long> lineVersions,
    /** 逐行分配明细 */
    List<AllocationPreviewResponse.Allocation> allocations,
    /** 已占用或本次实际使用的金额 */
    BigDecimal usedAmount,
    /** 本次请求金额中未分配的部分 */
    BigDecimal unallocatedAmount,
    /** 当前预览所依赖数据的摘要 */
    String fingerprint){
 public SettlementPreviewResponse{lineVersions=Map.copyOf(lineVersions);allocations=List.copyOf(allocations);}
}
