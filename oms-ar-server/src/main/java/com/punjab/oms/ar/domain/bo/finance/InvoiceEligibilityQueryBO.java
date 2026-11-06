package com.punjab.oms.ar.domain.bo.finance;
import java.util.List;
/** 开票归属范围，Query经转换后进入DAO。 @author Punjab */
public record InvoiceEligibilityQueryBO(
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId,
    /** 应收所属客户ID */
    Long customerId,
    /** 结算币种代码 */
    String currency,
    /** 选定的业务组ID集合 */
    List<Long> groupIds){}
