package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.util.List;import com.punjab.oms.ar.enums.InvoiceKind;
/** 拟关联发票的完整分配，金额按蓝正红负传入。 @author Punjab */
public record InvoicePreviewRequest(@NotNull @Positive Long legalEntityId,@NotNull @Positive Long customerId,@NotBlank @Size(min=3,max=3) String currency,
 @NotNull InvoiceKind kind,@Positive Long originalInvoiceId,@NotNull @Digits(integer=16,fraction=8) BigDecimal totalAmount,
 @NotEmpty @Size(max=500) List<@NotNull @Valid Line> lines){
 /** 一条外部发票行可分配给多条应收，红字可再分到多条原关联。 @author Punjab */
 public record Line(@NotBlank @Size(max=128) String invoiceLineId,@NotNull @Positive Long receivableLineId,
  @NotNull @Digits(integer=16,fraction=8) BigDecimal amount,@DecimalMin("0") @Digits(integer=4,fraction=8) BigDecimal taxRate,@Positive Long originalAllocationId){}
}
