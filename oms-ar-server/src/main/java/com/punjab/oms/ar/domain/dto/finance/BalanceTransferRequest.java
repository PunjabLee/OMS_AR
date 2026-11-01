package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
/** 用户确认转入账户的负数余额，不产生实际收款。 @author Punjab */
public record BalanceTransferRequest(@NotBlank @Size(max=128) String requestKey,@NotNull @Positive Long legalEntityId,
 @NotNull @Positive Long negativeLineId,@NotNull @Positive BigDecimal amount,@NotNull @PositiveOrZero Long expectedLineVersion,
 @NotBlank @Size(max=500) String reason){}
