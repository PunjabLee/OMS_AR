package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
public final class CashHoldRequest {
    private CashHoldRequest(){}
    public record Freeze(@NotBlank @Size(max=128) String requestKey,@Positive @NotNull Long legalEntityId,@NotNull @Positive Long accountId,@NotNull @Positive @Digits(integer=16,fraction=8) BigDecimal amount,@NotBlank @Size(max=500) String reason){}
    public record Release(@NotBlank @Size(max=128) String requestKey,@Positive @NotNull Long legalEntityId,@NotNull @Positive Long accountId,@NotNull @Positive Long holdId,@NotNull @Positive @Digits(integer=16,fraction=8) BigDecimal amount,@NotBlank @Size(max=500) String reason){}
}
