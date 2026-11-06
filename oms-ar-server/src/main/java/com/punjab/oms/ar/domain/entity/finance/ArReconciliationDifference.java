package com.punjab.oms.ar.domain.entity.finance;
import lombok.Getter;import lombok.Setter;import java.math.BigDecimal;
/** SQL只读差异投影，无对应可写表。 @author Punjab */
@Getter @Setter public class ArReconciliationDifference {
    /** 对账差异对应的规则标识 */
    private String rule;
    /** 被记录或诊断的对象类型 */
    private String objectType;
    /** 被记录或诊断的对象ID */
    private Long objectId;
    /** 对账对象当前持久化的值 */
    private BigDecimal storedAmount;
    /** 对账规则计算的预期值 */
    private BigDecimal expectedAmount;}
