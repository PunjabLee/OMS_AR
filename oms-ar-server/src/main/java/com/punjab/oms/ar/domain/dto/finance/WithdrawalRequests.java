package com.punjab.oms.ar.domain.dto.finance;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/** 提现及受托收款档案请求。 @author Punjab */
public final class WithdrawalRequests {
    private WithdrawalRequests() {}
    public record Bind(@NotBlank @Size(max=128) String requestKey,@Positive @NotNull Long legalEntityId,
            @Positive @NotNull Long customerId,@Pattern(regexp="[A-Z]{3}") @NotNull String currency,
            @NotBlank @Size(max=100) String beneficiaryRef,@Pattern(regexp="[0-9]{4}") @NotNull String lastFour,
            @NotBlank @Size(max=500) String reason) {}
    public record Apply(@NotBlank @Size(max=128) String requestKey,@Positive @NotNull Long legalEntityId,
            @Positive @NotNull Long cashAccountId,@Positive @NotNull Long payoutAccountId,
            @NotNull @Positive @Digits(integer=16,fraction=8) BigDecimal amount,@NotBlank @Size(max=500) String reason) {}
    public record Action(@NotBlank @Size(max=128) String requestKey,@Positive @NotNull Long legalEntityId,
            @Positive @NotNull Long id,@NotBlank @Size(max=500) String reason) {}
    public enum Outcome { PAID, FAILED, UNKNOWN }
    public record Result(@NotBlank @Size(max=128) String requestKey,@Positive @NotNull Long legalEntityId,
            @Positive @NotNull Long id,@NotNull Outcome outcome,@Size(max=128) String bankReceipt,
            @Size(max=256) String evidence,@NotBlank @Size(max=500) String reason) {}
}
