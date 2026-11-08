package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import java.math.BigDecimal;
/** 客户信用余额持久化记录。 @author Punjab */
@TableName("oms_ar_balance_lot")
public class ArBalanceLot extends ArBaseEntity {
 /** 从信用账户查询得出的现金分户编号，未分户时为空。 */
 @com.baomidou.mybatisplus.annotation.TableField(exist=false)
 private Long cashAccountId;
 public Long getCashAccountId(){return cashAccountId;}
 public void setCashAccountId(Long value){cashAccountId=value;}
 /** 客户信用账户ID */
 private Long accountId;
 public Long getAccountId(){return accountId;} public void setAccountId(Long v){accountId=v;}
 /** 可选的来源行标识 */
 private Long sourceLineId;
 public Long getSourceLineId(){return sourceLineId;} public void setSourceLineId(Long v){sourceLineId=v;}
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
 /** 当前业务记录的状态 */
 private String status;
 public String getStatus(){return status;} public void setStatus(String v){status=v;}
 /** 本记录的业务金额，方向由业务类型决定 */
 private BigDecimal amount;
 public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
 /** 已占用或本次实际使用的金额 */
 private BigDecimal usedAmount;
 public BigDecimal getUsedAmount(){return usedAmount;} public void setUsedAmount(BigDecimal v){usedAmount=v;}
}
