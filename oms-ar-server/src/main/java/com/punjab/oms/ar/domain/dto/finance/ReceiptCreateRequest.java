package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
/** 收款登记，仅授权的收款适配方可调用。 @author Punjab */
public record ReceiptCreateRequest(
    /** 调用方提供的幂等请求键 */
    @NotBlank @Size(max=128) String requestKey,
 /** 应收所属客户ID */
 @NotNull @Positive Long customerId,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
 /** 结算币种代码 */
 @NotBlank @Size(min=3,max=3) String currency,
    /** 本记录的业务金额，方向由业务类型决定 */
    @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=16,fraction=8) BigDecimal amount,
 /** 实际收款日期 */
 @NotNull LocalDate receivedDate,
    /** 来源系统标识 */
    @NotBlank @Size(max=64) String sourceSystem,
 /** 来源单据ID */
 @NotBlank @Size(max=128) String sourceId,
    /** 来源单据编号 */
    @NotBlank @Size(max=128) String sourceNo,
    /** 分户范围必填的充值账户。 */
    @Positive @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    Long cashAccountId) {
    public ReceiptCreateRequest(String key,Long customer,Long scope,String currency,BigDecimal amount,
                                LocalDate date,String system,String source,String number) {
        this(key,customer,scope,currency,amount,date,system,source,number,null);
    }
}
