package com.punjab.oms.ar.domain.dto.finance;

import jakarta.validation.constraints.*;

/** 授权财务确认将原充值整批冲回。 @author Punjab */
public record CashDepositReverseRequest(
    /** 唯一操作键。 */ @NotBlank @Size(max=128) String requestKey,
    /** 核算主体。 */ @Positive @NotNull Long legalEntityId,
    /** 被冲正的原收款。 */ @Positive @NotNull Long receiptId,
    /** 原因。 */ @NotBlank @Size(max=500) String reason) {}
