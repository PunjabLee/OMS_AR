package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.util.List;import com.punjab.oms.ar.enums.InvoiceKind;
/** 拟关联发票的完整分配，金额按蓝正红负传入。 @author Punjab */
public record InvoicePreviewRequest(
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
    /** 应收所属客户ID */
    @NotNull @Positive Long customerId,
    /** 结算币种代码 */
    @NotBlank @Size(min=3,max=3) String currency,
 /** 发票关联方向 */
 @NotNull InvoiceKind kind,
    /** 红字引用的原蓝字关联ID */
    @Positive Long originalInvoiceId,
    /** 带符号的发票分配合计 */
    @NotNull @Digits(integer=16,fraction=8) BigDecimal totalAmount,
 /** 本次请求或结果涉及的明细 */
 @NotEmpty @Size(max=500) List<@NotNull @Valid Line> lines){
 /** 一条外部发票行可分配给多条应收，红字可再分到多条原关联。 @author Punjab */
 public record Line(
    /** 外部发票行标识 */
    @NotBlank @Size(max=128) String invoiceLineId,
    /** 分配所关联的应收行ID */
    @NotNull @Positive Long receivableLineId,
  /** 本记录的业务金额，方向由业务类型决定 */
  @NotNull @Digits(integer=16,fraction=8) BigDecimal amount,
    /** 税率，以小数表示，空值与零税率区分 */
    @DecimalMin("0") @Digits(integer=4,fraction=8) BigDecimal taxRate,
    /** 红字分配引用的原蓝字分配ID */
    @Positive Long originalAllocationId){}
}
