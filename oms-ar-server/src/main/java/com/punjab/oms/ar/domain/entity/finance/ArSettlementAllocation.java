package com.punjab.oms.ar.domain.entity.finance;
import java.math.BigDecimal;import com.baomidou.mybatisplus.annotation.TableName;
/** 核销持久化记录。 @author Punjab */
@TableName("oms_ar_settlement_allocation")
public class ArSettlementAllocation extends ArBaseEntity {
 private Long settlementId;
 public Long getSettlementId(){return settlementId;}
 public void setSettlementId(Long v){settlementId=v;}
 private Integer sequenceNo;
 public Integer getSequenceNo(){return sequenceNo;}
 public void setSequenceNo(Integer v){sequenceNo=v;}
 private Long positiveLineId;
 public Long getPositiveLineId(){return positiveLineId;}
 public void setPositiveLineId(Long v){positiveLineId=v;}
 private Long negativeLineId;
 public Long getNegativeLineId(){return negativeLineId;}
 public void setNegativeLineId(Long v){negativeLineId=v;}
 private String amountType;
 public String getAmountType(){return amountType;}
 public void setAmountType(String v){amountType=v;}
 private BigDecimal amount;
 public BigDecimal getAmount(){return amount;}
 public void setAmount(BigDecimal v){amount=v;}
}
