package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;import java.math.BigDecimal;
/** 余额核销来源追踪记录。 @author Punjab */
@TableName("oms_ar_balance_settlement")
public class ArBalanceSettlement extends ArBaseEntity {
 private Long settlementId;
 public Long getSettlementId(){return settlementId;} public void setSettlementId(Long v){settlementId=v;}
 private Long accountId;
 public Long getAccountId(){return accountId;} public void setAccountId(Long v){accountId=v;}
}
