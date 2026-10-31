package com.punjab.oms.ar.domain.entity.finance;
import java.math.BigDecimal;import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableName;
/** 真实收款记录与分配余额。 @author Punjab */
@TableName("oms_ar_receipt")
public class ArReceipt extends ArBaseEntity {
 private Long customerId;
 public Long getCustomerId(){return customerId;}
 public void setCustomerId(Long v){customerId=v;}
 private Long legalEntityId;
 public Long getLegalEntityId(){return legalEntityId;}
 public void setLegalEntityId(Long v){legalEntityId=v;}
 private String currency;
 public String getCurrency(){return currency;}
 public void setCurrency(String v){currency=v;}
 private BigDecimal amount;
 public BigDecimal getAmount(){return amount;}
 public void setAmount(BigDecimal v){amount=v;}
 private BigDecimal allocatedAmount;
 public BigDecimal getAllocatedAmount(){return allocatedAmount;}
 public void setAllocatedAmount(BigDecimal v){allocatedAmount=v;}
 private Long version;
 public Long getVersion(){return version;}
 public void setVersion(Long v){version=v;}
 private LocalDate receivedDate;
 public LocalDate getReceivedDate(){return receivedDate;}
 public void setReceivedDate(LocalDate v){receivedDate=v;}
 private String sourceSystem;
 public String getSourceSystem(){return sourceSystem;}
 public void setSourceSystem(String v){sourceSystem=v;}
 private String sourceId;
 public String getSourceId(){return sourceId;}
 public void setSourceId(String v){sourceId=v;}
 private String sourceNo;
 public String getSourceNo(){return sourceNo;}
 public void setSourceNo(String v){sourceNo=v;}
}
