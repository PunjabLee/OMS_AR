package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;
/** 差异定位与存储/重算金额，不提供自动修复。 @author Punjab */
public record ReconciliationResponse(String rule,String objectType,Long objectId,BigDecimal storedAmount,BigDecimal expectedAmount){}
