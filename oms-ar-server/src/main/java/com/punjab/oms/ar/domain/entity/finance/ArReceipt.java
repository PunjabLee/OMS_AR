package com.punjab.oms.ar.domain.entity.finance;
import java.math.BigDecimal;import java.time.LocalDate;
import com.baomidou.mybatisplus.annotation.TableName;
/** 真实收款记录与分配余额。 @author Punjab */
@TableName("oms_ar_receipt")
public class ArReceipt extends ArBaseEntity {
 /** 应收所属客户ID */
 private Long customerId;
 public Long getCustomerId(){return customerId;}
 public void setCustomerId(Long v){customerId=v;}
 /** 核算主体ID，也是权限范围 */
 private Long legalEntityId;
 public Long getLegalEntityId(){return legalEntityId;}
 public void setLegalEntityId(Long v){legalEntityId=v;}
 /** 结算币种代码 */
 private String currency;
 public String getCurrency(){return currency;}
 public void setCurrency(String v){currency=v;}
 /** 本记录的业务金额，方向由业务类型决定 */
 private BigDecimal amount;
 public BigDecimal getAmount(){return amount;}
 public void setAmount(BigDecimal v){amount=v;}
 /** 收款已用于核销的金额 */
 private BigDecimal allocatedAmount;
 public BigDecimal getAllocatedAmount(){return allocatedAmount;}
 public void setAllocatedAmount(BigDecimal v){allocatedAmount=v;}
 /** 用于并发校验的记录版本 */
 private Long version;
 public Long getVersion(){return version;}
 public void setVersion(Long v){version=v;}
 /** 实际收款日期 */
 private LocalDate receivedDate;
 public LocalDate getReceivedDate(){return receivedDate;}
 public void setReceivedDate(LocalDate v){receivedDate=v;}
 /** 来源系统标识 */
 private String sourceSystem;
 public String getSourceSystem(){return sourceSystem;}
 public void setSourceSystem(String v){sourceSystem=v;}
 /** 来源单据ID */
 private String sourceId;
 public String getSourceId(){return sourceId;}
 public void setSourceId(String v){sourceId=v;}
 /** 来源单据编号 */
 private String sourceNo;
 public String getSourceNo(){return sourceNo;}
 public void setSourceNo(String v){sourceNo=v;}
}
