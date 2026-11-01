package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;import java.util.List;
/** 只读额度，行剩余额不可越过同组/类型/税率共享额度叠加使用。 @author Punjab */
public record InvoiceEligibilityResponse(List<Bucket> buckets,List<Line> lines,String fingerprint){
 public InvoiceEligibilityResponse{buckets=List.copyOf(buckets);lines=List.copyOf(lines);}
 /** 正负应收及已开票均带原方向，负的remainingNetAmount不等于可红字额度。 @author Punjab */
 public record Bucket(Long groupId,String amountType,BigDecimal taxRate,BigDecimal receivableNetAmount,BigDecimal invoicedNetAmount,BigDecimal remainingNetAmount,BigDecimal blueAvailableAmount){}
 /** 单行额度仅提供自身上限，实际蓝字还需共享bucket限制；红字还需原发票分配限制。 @author Punjab */
 public record Line(Long id,Long physicalGroupId,Long invoiceGroupId,String amountType,BigDecimal taxRate,BigDecimal receivableAmount,BigDecimal invoicedAmount,BigDecimal remainingAmount,Long version,boolean invoiceSupported,String blockedReason){}
}
