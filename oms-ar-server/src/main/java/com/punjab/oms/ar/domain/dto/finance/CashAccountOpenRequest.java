package com.punjab.oms.ar.domain.dto.finance;

import jakarta.validation.constraints.*;

/** 财务开户请求。 @author Punjab */
public record CashAccountOpenRequest(
    /** 请求幂等键。 */ @NotBlank @Size(max=128) String requestKey,
    /** 核算主体。 */ @NotNull @Positive Long legalEntityId,
    /** 客户标识。 */ @NotNull @Positive Long customerId,
    /** 结算币种。 */ @NotBlank @Pattern(regexp="[A-Z]{3}") String currency,
    /** 账户开票属性。 */ @NotBlank @Pattern(regexp="INVOICE|NO_INVOICE") String invoiceMode,
    /** 开户事由。 */ @NotBlank @Size(max=500) String reason) {}
