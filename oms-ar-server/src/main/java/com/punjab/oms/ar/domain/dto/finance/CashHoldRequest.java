package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public final class CashHoldRequest {
    private CashHoldRequest(){}
    public record Freeze(
        /** 请求幂等键，相同请求可安全重放。 */ @NotBlank @Size(max=128) String requestKey,
        /** 现金账户所属核算主体编号。 */ @Positive @NotNull Long legalEntityId,
        /** 现金账户编号。 */ @NotNull @Positive Long accountId,
        /** 申请冻结的现金金额。 */ @NotNull @Positive @Digits(integer=16,fraction=8) BigDecimal amount,
        /** 业务操作原因。 */ @NotBlank @Size(max=500) String reason){}
    public record Release(
        /** 请求幂等键，相同请求可安全重放。 */ @NotBlank @Size(max=128) String requestKey,
        /** 现金账户所属核算主体编号。 */ @Positive @NotNull Long legalEntityId,
        /** 现金账户编号。 */ @NotNull @Positive Long accountId,
        /** 原冻结单编号。 */ @NotNull @Positive Long holdId,
        /** 本次从原冻结单解冻的金额。 */ @NotNull @Positive @Digits(integer=16,fraction=8) BigDecimal amount,
        /** 业务操作原因。 */ @NotBlank @Size(max=500) String reason){}
}
