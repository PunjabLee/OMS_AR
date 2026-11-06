package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;import java.math.BigDecimal;
/** 用户确认转入账户的负数余额，不产生实际收款。 @author Punjab */
public record BalanceTransferRequest(
    /** 调用方提供的幂等请求键 */
    @NotBlank @Size(max=128) String requestKey,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
 /** 提供抵销额度的负数应收行ID */
 @NotNull @Positive Long negativeLineId,
    /** 本记录的业务金额，方向由业务类型决定 */
    @NotNull @Positive BigDecimal amount,
    /** 用户确认时的来源行版本 */
    @NotNull @PositiveOrZero Long expectedLineVersion,
 /** 业务操作原因 */
 @NotBlank @Size(max=500) String reason){}
