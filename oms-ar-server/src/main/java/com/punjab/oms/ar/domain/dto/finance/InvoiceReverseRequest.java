package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 撤销本系统关联，原记录保留，不代表税务平台作废。 @author Punjab */
public record InvoiceReverseRequest(
    /** 调用方提供的幂等请求键 */
    @NotBlank @Size(max=128) String requestKey,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
    /** 发票关联记录ID */
    @NotNull @Positive Long invoiceId,
    /** 业务操作原因 */
    @NotBlank @Size(max=500) String reason){}
