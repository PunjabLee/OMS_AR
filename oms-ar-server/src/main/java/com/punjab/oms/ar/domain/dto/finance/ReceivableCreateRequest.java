package com.punjab.oms.ar.domain.dto.finance;
import com.punjab.oms.ar.enums.AmountType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
/** 生效应收创建请求，不接受外部设置余额和审计人。 @author Punjab */
public record ReceivableCreateRequest(
 /** 调用方提供的幂等请求键 */
 @NotBlank @Size(max=128) String requestKey,
 /** 应收所属客户ID */
 @NotNull @Positive Long customerId,
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
 /** 结算币种代码 */
 @NotBlank @Size(min=3,max=3) String currency,
 /** 来源业务类别，例如出库或汇率差异 */
 @NotBlank @Size(max=32) String businessType,
 /** 业务发生日期 */
 @NotNull LocalDate businessDate,
    /** 应收到期日期 */
    LocalDate dueDate,
 /** 业务操作原因 */
 @NotBlank @Size(max=500) String reason,
 /** 本次请求或结果涉及的明细 */
 @NotEmpty @Size(max=500) List<@Valid Line> lines) {
 /** 新组按groupKey归并，已有组指定groupId。 @author Punjab */
 public record Line(
    /** 本次创建内归并明细的新组标识 */
    @NotBlank @Size(max=64) String groupKey,
    /** 业务组ID */
    @Positive Long groupId,
  /** 金额性质，区分价款和销项税 */
  @NotNull AmountType amountType,
    /** 本记录的业务金额，方向由业务类型决定 */
    @NotNull @Digits(integer=16,fraction=8) BigDecimal amount,
  /** 税率，以小数表示，空值与零税率区分 */
  @DecimalMin("0") @Digits(integer=4,fraction=8) BigDecimal taxRate,
  /** 上游确认的计税依据金额 */
  @Digits(integer=16,fraction=8) BigDecimal taxBaseAmount,
    /** 生成明细的来源信息 */
    @Valid Source source,
    /** 调整所指定的原正数应收行ID */
    @Positive Long originalLineId,
    /** 原币金额和汇率折算依据 */
    @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL) @Valid ExchangeBasis exchangeBasis) {
  public Line(String groupKey,Long groupId,AmountType amountType,BigDecimal amount,BigDecimal taxRate,BigDecimal taxBaseAmount,Source source,Long originalLineId){this(groupKey,groupId,amountType,amount,taxRate,taxBaseAmount,source,originalLineId,null);}
  public Line(String groupKey,Long groupId,AmountType amountType,BigDecimal amount,BigDecimal taxRate,BigDecimal taxBaseAmount,Source source){this(groupKey,groupId,amountType,amount,taxRate,taxBaseAmount,source,null);}
 }
 /** 汇率差异计算依据，差额以应收行amount为准并必须校验。 @author Punjab */
 public record ExchangeBasis(
    /** 折算前的原币种 */
    @NotBlank @Size(min=3,max=3) String originalCurrency,
  /** 原始金额 */
  @NotNull @Digits(integer=16,fraction=8) BigDecimal originalAmount,
  /** 原换算汇率 */
  @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=12) BigDecimal oldRate,
  /** 新的换算汇率 */
  @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=12,fraction=12) BigDecimal newRate,
  /** 汇率报价方向，决定乘法或除法折算 */
  @NotNull com.punjab.oms.ar.enums.ExchangeRateDirection rateDirection){}
 /** 来源行可空，事件和输出键不可空。 @author Punjab */
 public record Source(
    /** 来源系统标识 */
    @NotBlank @Size(max=64) String system,
    /** 来源单据或事件的类型 */
    @NotBlank @Size(max=64) String type,
  /** 来源单据编号 */
  @NotBlank @Size(max=128) String number,
    /** 来源单据ID */
    @NotBlank @Size(max=128) String id,
  /** 可选的上游单据行ID */
  @Size(max=128) String lineId,
    /** 事件标识 */
    @NotBlank @Size(max=128) String eventId,
  /** 区分同一来源事件中各输出的键 */
  @NotBlank @Size(max=64) String outputKey) {}
}
