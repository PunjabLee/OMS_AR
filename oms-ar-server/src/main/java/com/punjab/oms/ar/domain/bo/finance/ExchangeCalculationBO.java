package com.punjab.oms.ar.domain.bo.finance;
import java.math.BigDecimal;
/** 分别折算后按结算币种舍入的金额及差额。 @author Punjab */
public record ExchangeCalculationBO(BigDecimal oldSettlementAmount,BigDecimal newSettlementAmount,BigDecimal difference){}
