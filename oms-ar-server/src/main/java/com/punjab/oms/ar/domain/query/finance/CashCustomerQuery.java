package com.punjab.oms.ar.domain.query.finance;

import jakarta.validation.constraints.*;

/** 客户现金账户的精确结算范围。 @author Punjab */
public record CashCustomerQuery(
    /** 核算主体。 */ @NotNull @Positive Long legalEntityId,
    /** 客户。 */ @NotNull @Positive Long customerId,
    /** 结算币种。 */ @NotNull @Pattern(regexp="[A-Z]{3}") String currency) {}
