package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import java.math.BigDecimal;
/** 汇率差异原始计算依据，不随后续汇率变化重算。 @author Punjab */
@TableName("oms_ar_exchange_basis")
public class ArExchangeBasis extends ArBaseEntity {
 /** 业务明细行标识 */
 private Long lineId;
 public Long getLineId(){return lineId;} public void setLineId(Long value){lineId=value;}
 /** 折算前的原币种 */
 private String originalCurrency;
 public String getOriginalCurrency(){return originalCurrency;} public void setOriginalCurrency(String value){originalCurrency=value;}
 /** 汇率报价方向，决定乘法或除法折算 */
 private String rateDirection;
 public String getRateDirection(){return rateDirection;} public void setRateDirection(String value){rateDirection=value;}
 /** 原始金额 */
 private BigDecimal originalAmount;
 public BigDecimal getOriginalAmount(){return originalAmount;} public void setOriginalAmount(BigDecimal value){originalAmount=value;}
 /** 原换算汇率 */
 private BigDecimal oldRate;
 public BigDecimal getOldRate(){return oldRate;} public void setOldRate(BigDecimal value){oldRate=value;}
 /** 新的换算汇率 */
 private BigDecimal newRate;
 public BigDecimal getNewRate(){return newRate;} public void setNewRate(BigDecimal value){newRate=value;}
 /** 按原汇率计算的结算币金额 */
 private BigDecimal oldSettlementAmount;
 public BigDecimal getOldSettlementAmount(){return oldSettlementAmount;} public void setOldSettlementAmount(BigDecimal value){oldSettlementAmount=value;}
 /** 按新汇率计算的结算币金额 */
 private BigDecimal newSettlementAmount;
 public BigDecimal getNewSettlementAmount(){return newSettlementAmount;} public void setNewSettlementAmount(BigDecimal value){newSettlementAmount=value;}
 /** 以结算币种确认的汇率差额 */
 private BigDecimal confirmedDifference;
 public BigDecimal getConfirmedDifference(){return confirmedDifference;} public void setConfirmedDifference(BigDecimal value){confirmedDifference=value;}
}
