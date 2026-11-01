package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;import java.util.List;import com.punjab.oms.ar.enums.InvoiceKind;
/** 通过额度/税率校验的分配，不占用开票额度。 @author Punjab */
public record InvoicePreviewResponse(InvoiceKind kind,BigDecimal totalAmount,List<Allocation> allocations,String fingerprint){
 public InvoicePreviewResponse{allocations=List.copyOf(allocations);}
 /** 明确原应收版本、开票归属与原红字关联。 @author Punjab */
 public record Allocation(String invoiceLineId,Long receivableLineId,Long invoiceGroupId,String amountType,BigDecimal amount,BigDecimal taxRate,Long lineVersion,Long originalAllocationId){}
}
