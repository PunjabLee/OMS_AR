package com.punjab.oms.ar.domain.vo.finance;
/** 信用余额只读响应。 @author Punjab */
public record CustomerAccountResponse(
    /** 当前对象ID */
    Long id,
    /** 应收所属客户ID */
    Long customerId,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId,
    /** 结算币种代码 */
    String currency,
    /** 客户信用账户可用余额 */
    java.math.BigDecimal balanceAmount,
    /** 用于并发校验的记录版本 */
    Long version){}
