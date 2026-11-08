package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import java.math.BigDecimal;
/** 客户信用余额持久化记录。 @author Punjab */
@TableName("oms_ar_customer_account")
public class ArCustomerAccount extends ArBaseEntity {
 private Long cashAccountId;
 public Long getCashAccountId(){return cashAccountId;}
 public void setCashAccountId(Long value){cashAccountId=value;}
 /** 应收所属客户ID */
 private Long customerId;
 public Long getCustomerId(){return customerId;} public void setCustomerId(Long v){customerId=v;}
 /** 核算主体ID，也是权限范围 */
 private Long legalEntityId;
 public Long getLegalEntityId(){return legalEntityId;} public void setLegalEntityId(Long v){legalEntityId=v;}
 /** 用于并发校验的记录版本 */
 private Long version;
 public Long getVersion(){return version;} public void setVersion(Long v){version=v;}
 /** 结算币种代码 */
 private String currency;
 public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
 /** 客户信用账户可用余额 */
 private BigDecimal balanceAmount;
 public BigDecimal getBalanceAmount(){return balanceAmount;} public void setBalanceAmount(BigDecimal v){balanceAmount=v;}
}
