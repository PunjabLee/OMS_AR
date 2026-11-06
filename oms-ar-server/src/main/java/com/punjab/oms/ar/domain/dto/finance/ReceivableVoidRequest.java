package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 财务整单作废，无审批但必须说明原因。 @author Punjab */
public record ReceivableVoidRequest(
    /** 调用方提供的幂等请求键 */
    @NotBlank @Size(max=128) String requestKey,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
    /** 应收单ID */
    @NotNull @Positive Long documentId,
    /** 业务操作原因 */
    @NotBlank @Size(max=500) String reason){}
