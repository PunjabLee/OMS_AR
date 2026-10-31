package com.punjab.oms.ar.domain.vo.finance;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
/** 应收详情。 @author Punjab */
public record ReceivableResponse(Document document,String settlementStatus,List<Line> lines,Amounts principal,Amounts outputTax){
 public ReceivableResponse{lines=List.copyOf(lines);}
 /** 单据摘要。 @author Punjab */
 public record Document(Long id,String docNo,Long customerId,Long legalEntityId,String currency,String businessType,String status,LocalDate businessDate,LocalDate dueDate){}
 /** 明细。 @author Punjab */
 public record Line(Long id,Long groupId,Integer lineNo,String amountType,String status,BigDecimal receivableAmount,BigDecimal settledAmount,BigDecimal transferredAmount,BigDecimal openAmount,BigDecimal invoicedAmount,BigDecimal taxRate,BigDecimal taxBaseAmount,Source source){}
 /** 来源。 @author Punjab */
 public record Source(String sourceSystem,String sourceType,String sourceNo,String sourceId,String sourceLineId,String sourceEventId,String sourceOutputKey){}
 /** 相同金额类型的各口径。 @author Punjab */
 public record Amounts(BigDecimal receivableAmount,BigDecimal settledAmount,BigDecimal transferredAmount,BigDecimal openAmount,BigDecimal invoicedAmount){}
}
