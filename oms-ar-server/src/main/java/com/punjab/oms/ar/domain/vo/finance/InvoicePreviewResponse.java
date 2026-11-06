package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;import java.util.List;import com.punjab.oms.ar.enums.InvoiceKind;
/** 通过额度/税率校验的分配，不占用开票额度。 @author Punjab */
public record InvoicePreviewResponse(
    /** 发票关联方向 */
    InvoiceKind kind,
    /** 带符号的发票分配合计 */
    BigDecimal totalAmount,
    /** 逐行分配明细 */
    List<Allocation> allocations,
    /** 当前预览所依赖数据的摘要 */
    String fingerprint){
 public InvoicePreviewResponse{allocations=List.copyOf(allocations);}
 /** 明确原应收版本、开票归属与原红字关联。 @author Punjab */
 public record Allocation(
    /** 外部发票行标识 */
    String invoiceLineId,
    /** 分配所关联的应收行ID */
    Long receivableLineId,
    /** 计算共享开票额度所用的业务组ID */
    Long invoiceGroupId,
    /** 金额性质，区分价款和销项税 */
    String amountType,
    /** 本记录的业务金额，方向由业务类型决定 */
    BigDecimal amount,
    /** 税率，以小数表示，空值与零税率区分 */
    BigDecimal taxRate,
    /** 应收行版本 */
    Long lineVersion,
    /** 红字分配引用的原蓝字分配ID */
    Long originalAllocationId){}
}
