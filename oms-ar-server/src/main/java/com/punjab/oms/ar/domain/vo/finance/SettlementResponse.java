package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.List;
/** 原分配和来源批次，反核销后仍保留。 @author Punjab */
public record SettlementResponse(Header header,Long accountId,List<Allocation> allocations,List<Usage> usages){
 public SettlementResponse{allocations=List.copyOf(allocations);usages=List.copyOf(usages);}
 /** 核销摘要。 @author Punjab */
 public record Header(Long id,String settlementNo,Long customerId,Long legalEntityId,String currency,String settlementType,Long receiptId,BigDecimal amount,String strategy,String status,String reason,Long version,LocalDateTime createdTime,String createdBy){}
 /** 双方应收及原始分配金额。 @author Punjab */
 public record Allocation(Long id,Integer sequenceNo,Long positiveLineId,Long negativeLineId,String amountType,BigDecimal amount){}
 /** 一笔分配可能使用多个批次。 @author Punjab */
 public record Usage(Long id,Long allocationId,Long lotId,Integer sequenceNo,BigDecimal amount){}
}
