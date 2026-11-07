package com.punjab.oms.ar.domain.dto.finance;

import jakarta.validation.constraints.*;

/** 现金账户状态变更，以调用方看到的版本确认。 @author Punjab */
public record CashStatusRequest(
        /** 操作幂等键。 */ @NotBlank @Size(max=128) String requestKey,
        /** 账户所属主体。 */ @Positive @NotNull Long legalEntityId,
        /** 目标账户。 */ @Positive @NotNull Long accountId,
        /** 操作前版本。 */ @PositiveOrZero @NotNull Long expectedVersion,
        /** 状态变更原因。 */ @NotBlank @Size(max=500) String reason) {}
