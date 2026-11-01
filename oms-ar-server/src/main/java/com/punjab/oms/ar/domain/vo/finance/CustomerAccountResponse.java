package com.punjab.oms.ar.domain.vo.finance;
/** 信用余额只读响应。 @author Punjab */
public record CustomerAccountResponse(Long id,Long customerId,Long legalEntityId,String currency,java.math.BigDecimal balanceAmount,Long version){}
