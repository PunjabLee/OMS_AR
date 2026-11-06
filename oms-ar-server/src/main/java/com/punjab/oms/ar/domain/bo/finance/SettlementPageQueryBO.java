package com.punjab.oms.ar.domain.bo.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;
/** 财务分页筛选范围。 @author Punjab */
@Getter @Setter public class SettlementPageQueryBO extends QueryPage {

    /** 核算主体ID，也是权限范围 */
    private Long legalEntityId;

    /** 应收所属客户ID */
    private Long customerId;

    /** 结算币种代码 */
    private String currency;

    /** 当前业务记录的状态 */
    private String status;

    /** 核销资金类型，区分实收、负数抵销和余额抵扣 */
    private String settlementType;

    /** 实际收款记录ID */
    private Long receiptId;
}
