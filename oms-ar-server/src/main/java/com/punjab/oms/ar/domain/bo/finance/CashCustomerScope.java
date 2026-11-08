package com.punjab.oms.ar.domain.bo.finance;

/** 客户现金账户的精确结算范围。 @author Punjab */
public record CashCustomerScope(
    /** 核算主体。 */ Long legalEntityId,
    /** 客户。 */ Long customerId,
    /** 结算币种。 */ String currency) {}
