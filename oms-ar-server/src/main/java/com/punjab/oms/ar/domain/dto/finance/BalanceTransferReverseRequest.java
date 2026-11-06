package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
/** 撤销未被使用的转入批次。 @author Punjab */
public record BalanceTransferReverseRequest(
    /** 调用方提供的幂等请求键 */
    @NotBlank @Size(max=128) String requestKey,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
 /** 信用余额来源批次ID */
 @NotNull @Positive Long lotId,
    /** 业务操作原因 */
    @NotBlank @Size(max=500) String reason){}
