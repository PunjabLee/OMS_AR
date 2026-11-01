package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import java.math.BigDecimal;import java.time.LocalDate;
/** 发票关联持久化记录。 @author Punjab */
@TableName("oms_ar_invoice")
public class ArInvoice extends ArBaseEntity {
 private String sourceSystem;
 public String getSourceSystem(){return sourceSystem;}public void setSourceSystem(String v){sourceSystem=v;}
 private String sourceId;
 public String getSourceId(){return sourceId;}public void setSourceId(String v){sourceId=v;}
 private String invoiceNo;
 public String getInvoiceNo(){return invoiceNo;}public void setInvoiceNo(String v){invoiceNo=v;}
 private String currency;
 public String getCurrency(){return currency;}public void setCurrency(String v){currency=v;}
 private String invoiceKind;
 public String getInvoiceKind(){return invoiceKind;}public void setInvoiceKind(String v){invoiceKind=v;}
 private String status;
 public String getStatus(){return status;}public void setStatus(String v){status=v;}
 private LocalDate invoiceDate;
 public LocalDate getInvoiceDate(){return invoiceDate;}public void setInvoiceDate(LocalDate v){invoiceDate=v;}
 private Long customerId;
 public Long getCustomerId(){return customerId;}public void setCustomerId(Long v){customerId=v;}
 private Long legalEntityId;
 public Long getLegalEntityId(){return legalEntityId;}public void setLegalEntityId(Long v){legalEntityId=v;}
 private Long originalInvoiceId;
 public Long getOriginalInvoiceId(){return originalInvoiceId;}public void setOriginalInvoiceId(Long v){originalInvoiceId=v;}
 private Long version;
 public Long getVersion(){return version;}public void setVersion(Long v){version=v;}
 private BigDecimal totalAmount;
 public BigDecimal getTotalAmount(){return totalAmount;}public void setTotalAmount(BigDecimal v){totalAmount=v;}
}
