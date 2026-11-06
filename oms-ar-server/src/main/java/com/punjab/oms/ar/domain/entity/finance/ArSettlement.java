package com.punjab.oms.ar.domain.entity.finance;
import java.math.BigDecimal;import com.baomidou.mybatisplus.annotation.TableName;
/** 核销持久化记录。 @author Punjab */
@TableName("oms_ar_settlement")
public class ArSettlement extends ArBaseEntity {
 /** 核销单编号 */
 private String settlementNo;
 public String getSettlementNo(){return settlementNo;}
 public void setSettlementNo(String v){settlementNo=v;}
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
 /** 核销资金类型，区分实收、负数抵销和余额抵扣 */
 private String settlementType;
 public String getSettlementType(){return settlementType;}
 public void setSettlementType(String v){settlementType=v;}
 /** 实际收款记录ID */
 private Long receiptId;
 public Long getReceiptId(){return receiptId;}
 public void setReceiptId(Long v){receiptId=v;}
 /** 本记录的业务金额，方向由业务类型决定 */
 private BigDecimal amount;
 public BigDecimal getAmount(){return amount;}
 public void setAmount(BigDecimal v){amount=v;}
 /** 用户选定的核销分配策略 */
 private String strategy;
 public String getStrategy(){return strategy;}
 public void setStrategy(String v){strategy=v;}
 /** 确认时记录的分配预览指纹 */
 private String previewFingerprint;
 public String getPreviewFingerprint(){return previewFingerprint;}
 public void setPreviewFingerprint(String v){previewFingerprint=v;}
 /** 当前业务记录的状态 */
 private String status;
 public String getStatus(){return status;}
 public void setStatus(String v){status=v;}
 /** 业务操作原因 */
 private String reason;
 public String getReason(){return reason;}
 public void setReason(String v){reason=v;}
 /** 用于并发校验的记录版本 */
 private Long version;
 public Long getVersion(){return version;}
 public void setVersion(Long v){version=v;}
}
