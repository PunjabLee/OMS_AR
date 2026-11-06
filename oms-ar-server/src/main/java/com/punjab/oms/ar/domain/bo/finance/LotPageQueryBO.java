package com.punjab.oms.ar.domain.bo.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;
/** 财务分页筛选范围。 @author Punjab */
@Getter @Setter public class LotPageQueryBO extends QueryPage {

    /** 核算主体ID，也是权限范围 */
    private Long legalEntityId;

    /** 应收所属客户ID */
    private Long customerId;

    /** 结算币种代码 */
    private String currency;

    /** 当前业务记录的状态 */
    private String status;

    /** 客户信用账户ID */
    private Long accountId;

    /** 可选的来源行标识 */
    private Long sourceLineId;
}
