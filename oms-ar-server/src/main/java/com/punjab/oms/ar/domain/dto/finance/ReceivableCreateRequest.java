package com.punjab.oms.ar.domain.dto.finance;
import com.punjab.oms.ar.enums.AmountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
/** 生效应收创建请求，不接受外部设置余额和审计人。 @author Punjab */
public record ReceivableCreateRequest(
 @NotBlank @Size(max=128) String requestKey,
 @NotNull @Positive Long customerId, @NotNull @Positive Long legalEntityId,
 @NotBlank @Size(min=3,max=3) String currency,
 @NotBlank @Size(max=32) String businessType,
 @NotNull LocalDate businessDate, LocalDate dueDate,
 @NotBlank @Size(max=500) String reason,
 @NotEmpty @Size(max=500) List<@Valid Line> lines) {
 /** 新组按groupKey归并，已有组指定groupId。 @author Punjab */
 public record Line(@NotBlank @Size(max=64) String groupKey, @Positive Long groupId,
  @NotNull AmountType amountType, @NotNull @Digits(integer=16,fraction=8) BigDecimal amount,
  @DecimalMin("0") @Digits(integer=4,fraction=8) BigDecimal taxRate,
  @Digits(integer=16,fraction=8) BigDecimal taxBaseAmount, @Valid Source source, @Positive Long originalLineId, @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) @Valid ExchangeBasis exchangeBasis) {
  public Line(String groupKey,Long groupId,AmountType amountType,BigDecimal amount,BigDecimal taxRate,BigDecimal taxBaseAmount,Source source,Long originalLineId){this(groupKey,groupId,amountType,amount,taxRate,taxBaseAmount,source,originalLineId,null);}
  public Line(String groupKey,Long groupId,AmountType amountType,BigDecimal amount,BigDecimal taxRate,BigDecimal taxBaseAmount,Source source){this(groupKey,groupId,amountType,amount,taxRate,taxBaseAmount,source,null);}
 }
 /** 汇率差异计算依据，差额以应收行amount为准并必须校验。 @author Punjab */
 public record ExchangeBasis(@NotBlank @Size(min=3,max=3) String originalCurrency,
  @NotNull @Digits(integer=16,fraction=8) BigDecimal originalAmount,
  @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=12) BigDecimal oldRate,
  @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=12) BigDecimal newRate,
  @NotNull com.punjab.oms.ar.enums.ExchangeRateDirection rateDirection){}
 /** 来源行可空，事件和输出键不可空。 @author Punjab */
 public record Source(@NotBlank @Size(max=64) String system, @NotBlank @Size(max=64) String type,
  @NotBlank @Size(max=128) String number, @NotBlank @Size(max=128) String id,
  @Size(max=128) String lineId, @NotBlank @Size(max=128) String eventId,
  @NotBlank @Size(max=64) String outputKey) {}
}
