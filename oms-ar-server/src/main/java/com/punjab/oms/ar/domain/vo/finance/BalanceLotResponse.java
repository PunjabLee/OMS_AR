package com.punjab.oms.ar.domain.vo.finance;
/** 信用余额只读响应。 @author Punjab */
public record BalanceLotResponse(Long id,Long accountId,Long sourceLineId,Long customerId,Long legalEntityId,String currency,java.math.BigDecimal amount,java.math.BigDecimal usedAmount,String status,Long version){}
