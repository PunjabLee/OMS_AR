package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import java.math.BigDecimal;import java.time.LocalDate;
/** 发票关联持久化记录。 @author Punjab */
@TableName("oms_ar_invoice_allocation")
public class ArInvoiceAllocation extends ArBaseEntity {
 private Long invoiceId;
 public Long getInvoiceId(){return invoiceId;}public void setInvoiceId(Long v){invoiceId=v;}
 private Long receivableLineId;
 public Long getReceivableLineId(){return receivableLineId;}public void setReceivableLineId(Long v){receivableLineId=v;}
 private Long originalInvoiceId;
 public Long getOriginalInvoiceId(){return originalInvoiceId;}public void setOriginalInvoiceId(Long v){originalInvoiceId=v;}
 private Long originalAllocationId;
 public Long getOriginalAllocationId(){return originalAllocationId;}public void setOriginalAllocationId(Long v){originalAllocationId=v;}
 private String invoiceKind;
 public String getInvoiceKind(){return invoiceKind;}public void setInvoiceKind(String v){invoiceKind=v;}
 private String invoiceLineId;
 public String getInvoiceLineId(){return invoiceLineId;}public void setInvoiceLineId(String v){invoiceLineId=v;}
 private String amountType;
 public String getAmountType(){return amountType;}public void setAmountType(String v){amountType=v;}
 private BigDecimal amount;
 public BigDecimal getAmount(){return amount;}public void setAmount(BigDecimal v){amount=v;}
 private BigDecimal taxRate;
 public BigDecimal getTaxRate(){return taxRate;}public void setTaxRate(BigDecimal v){taxRate=v;}
}
