package com.punjab.oms.ar.domain.entity.finance;
import java.math.BigDecimal;import com.baomidou.mybatisplus.annotation.TableName;
/** 核销持久化记录。 @author Punjab */
@TableName("oms_ar_settlement")
public class ArSettlement extends ArBaseEntity {
 private String settlementNo;
 public String getSettlementNo(){return settlementNo;}
 public void setSettlementNo(String v){settlementNo=v;}
 private Long customerId;
 public Long getCustomerId(){return customerId;}
 public void setCustomerId(Long v){customerId=v;}
 private Long legalEntityId;
 public Long getLegalEntityId(){return legalEntityId;}
 public void setLegalEntityId(Long v){legalEntityId=v;}
 private String currency;
 public String getCurrency(){return currency;}
 public void setCurrency(String v){currency=v;}
 private String settlementType;
 public String getSettlementType(){return settlementType;}
 public void setSettlementType(String v){settlementType=v;}
 private Long receiptId;
 public Long getReceiptId(){return receiptId;}
 public void setReceiptId(Long v){receiptId=v;}
 private BigDecimal amount;
 public BigDecimal getAmount(){return amount;}
 public void setAmount(BigDecimal v){amount=v;}
 private String strategy;
 public String getStrategy(){return strategy;}
 public void setStrategy(String v){strategy=v;}
 private String previewFingerprint;
 public String getPreviewFingerprint(){return previewFingerprint;}
 public void setPreviewFingerprint(String v){previewFingerprint=v;}
 private String status;
 public String getStatus(){return status;}
 public void setStatus(String v){status=v;}
 private String reason;
 public String getReason(){return reason;}
 public void setReason(String v){reason=v;}
 private Long version;
 public Long getVersion(){return version;}
 public void setVersion(Long v){version=v;}
}
