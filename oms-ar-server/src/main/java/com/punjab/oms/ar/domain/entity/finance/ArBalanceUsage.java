package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import java.math.BigDecimal;
/** 余额核销来源追踪记录。 @author Punjab */
@TableName("oms_ar_balance_usage")
public class ArBalanceUsage extends ArBaseEntity {
 /** 核销单ID */
 private Long settlementId;
 public Long getSettlementId(){return settlementId;} public void setSettlementId(Long v){settlementId=v;}
 /** 业务分配记录ID */
 private Long allocationId;
 public Long getAllocationId(){return allocationId;} public void setAllocationId(Long v){allocationId=v;}
 /** 信用余额来源批次ID */
 private Long lotId;
 public Long getLotId(){return lotId;} public void setLotId(Long v){lotId=v;}
 /** 分配或使用明细的顺序号 */
 private Integer sequenceNo;
 public Integer getSequenceNo(){return sequenceNo;} public void setSequenceNo(Integer v){sequenceNo=v;}
 /** 本记录的业务金额，方向由业务类型决定 */
 private BigDecimal amount;
 public BigDecimal getAmount(){return amount;} public void setAmount(BigDecimal v){amount=v;}
}
