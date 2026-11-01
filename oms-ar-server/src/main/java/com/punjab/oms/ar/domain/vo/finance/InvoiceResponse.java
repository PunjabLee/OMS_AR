package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;import java.time.LocalDate;import java.util.List;
/** 发票关联详情，红字余额按原分配逐行展示。 @author Punjab */
public record InvoiceResponse(Header header,List<Allocation> allocations){
 public InvoiceResponse{allocations=List.copyOf(allocations);}
 /** 来源与状态。 @author Punjab */
 public record Header(Long id,String sourceSystem,String sourceId,String invoiceNo,LocalDate invoiceDate,Long customerId,Long legalEntityId,String currency,String invoiceKind,Long originalInvoiceId,BigDecimal totalAmount,String status,Long version){}
 /** 红字关联保留原分配ID；redAvailableAmount只用于有效蓝字。 @author Punjab */
 public record Allocation(Long id,String invoiceLineId,Long receivableLineId,String amountType,BigDecimal amount,BigDecimal taxRate,Long originalAllocationId,BigDecimal redAvailableAmount){}
}
