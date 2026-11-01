package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 撤销本系统关联，原记录保留，不代表税务平台作废。 @author Punjab */
public record InvoiceReverseRequest(@NotBlank @Size(max=128) String requestKey,@NotNull @Positive Long legalEntityId,@NotNull @Positive Long invoiceId,@NotBlank @Size(max=500) String reason){}
