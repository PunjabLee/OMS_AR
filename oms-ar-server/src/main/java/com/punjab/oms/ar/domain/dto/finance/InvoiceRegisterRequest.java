package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.time.LocalDate;
/** 登记外部发票关联，不直接调用税务开票平台。 @author Punjab */
public record InvoiceRegisterRequest(@NotBlank @Size(max=128) String requestKey,@NotBlank @Size(max=64) String sourceSystem,
 @NotBlank @Size(max=128) String sourceId,@NotBlank @Size(max=128) String invoiceNo,@NotNull LocalDate invoiceDate,
 @NotNull @Valid InvoicePreviewRequest preview,@NotBlank @Size(min=64,max=64) String expectedFingerprint,@NotBlank @Size(max=500) String reason){}
