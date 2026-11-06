package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;
/** 差异定位与存储/重算金额，不提供自动修复。 @author Punjab */
public record ReconciliationResponse(
    /** 对账差异对应的规则标识 */
    String rule,
    /** 被记录或诊断的对象类型 */
    String objectType,
    /** 被记录或诊断的对象ID */
    Long objectId,
    /** 对账对象当前持久化的值 */
    BigDecimal storedAmount,
    /** 对账规则计算的预期值 */
    BigDecimal expectedAmount){}
