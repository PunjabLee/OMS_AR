package com.punjab.oms.ar.domain.bo.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;
/** 对账持久层范围。 @author Punjab */
@Getter @Setter public class ReconciliationQueryBO extends QueryPage {
    /** 核算主体ID，也是权限范围 */
    private Long legalEntityId;
    /** 应收所属客户ID */
    private Long customerId;
    /** 结算币种代码 */
    private String currency;public long getOffset(){return ((long)getPage()-1)*getSize();}}
