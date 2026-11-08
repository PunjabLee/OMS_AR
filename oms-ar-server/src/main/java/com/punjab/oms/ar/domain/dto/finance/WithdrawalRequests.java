package com.punjab.oms.ar.domain.dto.finance;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** 提现及受托收款档案请求。 @author Punjab */
public final class WithdrawalRequests {
    private WithdrawalRequests() {}
    public record Bind(
        /** 请求幂等键，相同请求可安全重放。 */ @NotBlank @Size(max=128) String requestKey,
        /** 现金账户所属核算主体编号。 */ @Positive @NotNull Long legalEntityId,
        /** 受托账户所属客户编号。 */ @Positive @NotNull Long customerId,
        /** 结算币种代码。 */ @Pattern(regexp="[A-Z]{3}") @NotNull String currency,
        /** 受托收款人的外部档案引用。 */ @NotBlank @Size(max=100) String beneficiaryRef,
        /** 受托收款账号末四位。 */ @Pattern(regexp="[0-9]{4}") @NotNull String lastFour,
        /** 业务操作原因。 */ @NotBlank @Size(max=500) String reason) {}
    public record Apply(
        /** 请求幂等键，相同请求可安全重放。 */ @NotBlank @Size(max=128) String requestKey,
        /** 现金账户所属核算主体编号。 */ @Positive @NotNull Long legalEntityId,
        /** 资金归属的现金账户编号。 */ @Positive @NotNull Long cashAccountId,
        /** 接收提现款项的受托账户编号。 */ @Positive @NotNull Long payoutAccountId,
        /** 本次申请提现的金额。 */ @NotNull @Positive @Digits(integer=16,fraction=8) BigDecimal amount,
        /** 业务操作原因。 */ @NotBlank @Size(max=500) String reason) {}
    public record Action(
        /** 请求幂等键，相同请求可安全重放。 */ @NotBlank @Size(max=128) String requestKey,
        /** 现金账户所属核算主体编号。 */ @Positive @NotNull Long legalEntityId,
        /** 提现申请编号。 */ @Positive @NotNull Long id,
        /** 业务操作原因。 */ @NotBlank @Size(max=500) String reason) {}
    public enum Outcome { PAID, FAILED, UNKNOWN }
    public record Result(
        /** 请求幂等键，相同请求可安全重放。 */ @NotBlank @Size(max=128) String requestKey,
        /** 现金账户所属核算主体编号。 */ @Positive @NotNull Long legalEntityId,
        /** 提现申请编号。 */ @Positive @NotNull Long id,
        /** 线下付款结果，可为成功、失败或待核实。 */ @NotNull Outcome outcome,
        /** 支付成功后登记的银行回单号。 */ @Size(max=128) String bankReceipt,
        /** 线下核实付款结果的凭证。 */ @Size(max=256) String evidence,
        /** 业务操作原因。 */ @NotBlank @Size(max=500) String reason) {}
}
