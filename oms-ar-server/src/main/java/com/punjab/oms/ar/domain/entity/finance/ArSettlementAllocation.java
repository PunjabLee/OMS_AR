package com.punjab.oms.ar.domain.entity.finance;
import java.math.BigDecimal;import com.baomidou.mybatisplus.annotation.TableName;
/** 核销持久化记录。 @author Punjab */
@TableName("oms_ar_settlement_allocation")
public class ArSettlementAllocation extends ArBaseEntity {
 /** 核销单ID */
 private Long settlementId;
 public Long getSettlementId(){return settlementId;}
 public void setSettlementId(Long v){settlementId=v;}
 /** 分配或使用明细的顺序号 */
 private Integer sequenceNo;
 public Integer getSequenceNo(){return sequenceNo;}
 public void setSequenceNo(Integer v){sequenceNo=v;}
 /** 本次分配抵扣的正数应收行ID */
 private Long positiveLineId;
 public Long getPositiveLineId(){return positiveLineId;}
 public void setPositiveLineId(Long v){positiveLineId=v;}
 /** 提供抵销额度的负数应收行ID */
 private Long negativeLineId;
 public Long getNegativeLineId(){return negativeLineId;}
 public void setNegativeLineId(Long v){negativeLineId=v;}
 /** 金额性质，区分价款和销项税 */
 private String amountType;
 public String getAmountType(){return amountType;}
 public void setAmountType(String v){amountType=v;}
 /** 本记录的业务金额，方向由业务类型决定 */
 private BigDecimal amount;
 public BigDecimal getAmount(){return amount;}
 public void setAmount(BigDecimal v){amount=v;}
}
