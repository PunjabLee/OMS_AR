package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import java.math.BigDecimal;
/** 汇率差异原始计算依据，不随后续汇率变化重算。 @author Punjab */
@TableName("oms_ar_exchange_basis")
public class ArExchangeBasis extends ArBaseEntity {
 private Long lineId;
 public Long getLineId(){return lineId;} public void setLineId(Long value){lineId=value;}
 private String originalCurrency;
 public String getOriginalCurrency(){return originalCurrency;} public void setOriginalCurrency(String value){originalCurrency=value;}
 private String rateDirection;
 public String getRateDirection(){return rateDirection;} public void setRateDirection(String value){rateDirection=value;}
 private BigDecimal originalAmount;
 public BigDecimal getOriginalAmount(){return originalAmount;} public void setOriginalAmount(BigDecimal value){originalAmount=value;}
 private BigDecimal oldRate;
 public BigDecimal getOldRate(){return oldRate;} public void setOldRate(BigDecimal value){oldRate=value;}
 private BigDecimal newRate;
 public BigDecimal getNewRate(){return newRate;} public void setNewRate(BigDecimal value){newRate=value;}
 private BigDecimal oldSettlementAmount;
 public BigDecimal getOldSettlementAmount(){return oldSettlementAmount;} public void setOldSettlementAmount(BigDecimal value){oldSettlementAmount=value;}
 private BigDecimal newSettlementAmount;
 public BigDecimal getNewSettlementAmount(){return newSettlementAmount;} public void setNewSettlementAmount(BigDecimal value){newSettlementAmount=value;}
 private BigDecimal confirmedDifference;
 public BigDecimal getConfirmedDifference(){return confirmedDifference;} public void setConfirmedDifference(BigDecimal value){confirmedDifference=value;}
}
