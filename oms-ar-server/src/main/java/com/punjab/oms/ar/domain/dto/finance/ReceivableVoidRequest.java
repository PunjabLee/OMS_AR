package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 财务整单作废，无审批但必须说明原因。 @author Punjab */
public record ReceivableVoidRequest(@NotBlank @Size(max=128) String requestKey,@NotNull @Positive Long legalEntityId,@NotNull @Positive Long documentId,@NotBlank @Size(max=500) String reason){}
