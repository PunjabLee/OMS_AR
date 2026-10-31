package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
/** 收款登记，仅授权的收款适配方可调用。 @author Punjab */
public record ReceiptCreateRequest(@NotBlank @Size(max=128) String requestKey,
 @NotNull @Positive Long customerId,@NotNull @Positive Long legalEntityId,
 @NotBlank @Size(min=3,max=3) String currency,@NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=16,fraction=8) BigDecimal amount,
 @NotNull LocalDate receivedDate,@NotBlank @Size(max=64) String sourceSystem,
 @NotBlank @Size(max=128) String sourceId,@NotBlank @Size(max=128) String sourceNo){}
