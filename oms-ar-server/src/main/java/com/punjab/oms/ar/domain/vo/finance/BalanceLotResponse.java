package com.punjab.oms.ar.domain.vo.finance;
/** 信用余额只读响应。 @author Punjab */
public record BalanceLotResponse(
    /** 当前对象ID */
    Long id,
    /** 客户信用账户ID */
    Long accountId,
    /** 可选的来源行标识 */
    Long sourceLineId,
    /** 应收所属客户ID */
    Long customerId,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId,
    /** 结算币种代码 */
    String currency,
    /** 本记录的业务金额，方向由业务类型决定 */
    java.math.BigDecimal amount,
    /** 已占用或本次实际使用的金额 */
    java.math.BigDecimal usedAmount,
    /** 当前业务记录的状态 */
    String status,
    /** 用于并发校验的记录版本 */
    Long version,
    /** 归属现金账户，未分户时为空 */
    Long cashAccountId){}
