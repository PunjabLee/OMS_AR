package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 整单反核销。 @author Punjab */
public record SettlementReverseRequest(
    /** 调用方提供的幂等请求键 */
    @NotBlank @Size(max=128) String requestKey,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
 /** 核销单ID */
 @NotNull @Positive Long settlementId,
    /** 业务操作原因 */
    @NotBlank @Size(max=500) String reason){}
