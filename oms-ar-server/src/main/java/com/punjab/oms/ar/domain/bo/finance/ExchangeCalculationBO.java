package com.punjab.oms.ar.domain.bo.finance;
import java.math.BigDecimal;
/** 分别折算后按结算币种舍入的金额及差额。 @author Punjab */
public record ExchangeCalculationBO(
    /** 按原汇率计算的结算币金额 */
    BigDecimal oldSettlementAmount,
    /** 按新汇率计算的结算币金额 */
    BigDecimal newSettlementAmount,
    /** 新旧折算值之差 */
    BigDecimal difference){}
