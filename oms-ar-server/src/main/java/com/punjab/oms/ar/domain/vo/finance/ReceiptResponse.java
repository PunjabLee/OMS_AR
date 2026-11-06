package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;
import java.time.LocalDate;
/** 收款余额，不与客户信用余额混用。 @author Punjab */
public record ReceiptResponse(
    /** 当前对象ID */
    Long id,
    /** 应收所属客户ID */
    Long customerId,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId,
    /** 结算币种代码 */
    String currency,
    /** 本记录的业务金额，方向由业务类型决定 */
    BigDecimal amount,
    /** 收款已用于核销的金额 */
    BigDecimal allocatedAmount,
    /** 可用于分配的金额 */
    BigDecimal availableAmount,
    /** 用于并发校验的记录版本 */
    Long version,
    /** 实际收款日期 */
    LocalDate receivedDate,
    /** 来源系统标识 */
    String sourceSystem,
    /** 来源单据ID */
    String sourceId,
    /** 来源单据编号 */
    String sourceNo){}
