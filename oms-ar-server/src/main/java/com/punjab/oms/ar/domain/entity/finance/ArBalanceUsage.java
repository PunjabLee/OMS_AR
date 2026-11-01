package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import java.math.BigDecimal;
/** 余额核销来源追踪记录。 @author Punjab */
@TableName("oms_ar_balance_usage")
public class ArBalanceUsage extends ArBaseEntity {
 private Long settlementId;
 public Long getSettlementId(){return settlementId;} public void setSettlementId(Long v){settlementId=v;}
 private Long allocationId;
 public Long getAllocationId(){return allocationId;} public void setAllocationId(Long v){allocationId=v;}
 private Long lotId;
 public Long getLotId(){return lotId;} public void setLotId(Long v){lotId=v;}
 private Integer sequenceNo;
 public Integer getSequenceNo(){return sequenceNo;} public void setSequenceNo(Integer v){sequenceNo=v;}
 private BigDecimal amount;
 public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
}
