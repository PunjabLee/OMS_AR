package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import java.math.BigDecimal;
/** 客户信用余额持久化记录。 @author Punjab */
@TableName("oms_ar_balance_lot")
public class ArBalanceLot extends ArBaseEntity {
 private Long accountId;
 public Long getAccountId(){return accountId;} public void setAccountId(Long v){accountId=v;}
 private Long sourceLineId;
 public Long getSourceLineId(){return sourceLineId;} public void setSourceLineId(Long v){sourceLineId=v;}
 private Long customerId;
 public Long getCustomerId(){return customerId;} public void setCustomerId(Long v){customerId=v;}
 private Long legalEntityId;
 public Long getLegalEntityId(){return legalEntityId;} public void setLegalEntityId(Long v){legalEntityId=v;}
 private Long version;
 public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
 private String currency;
 public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
 private String status;
 public String getStatus(){return status;} public void setStatus(String v){status=v;}
 private BigDecimal amount;
 public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
 private BigDecimal usedAmount;
 public BigDecimal getUsedAmount(){return usedAmount;} public void setUsedAmount(BigDecimal v){usedAmount=v;}
}
