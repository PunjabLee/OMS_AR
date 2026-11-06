package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;import java.time.LocalDate;import java.util.List;
/** 发票关联详情，红字余额按原分配逐行展示。 @author Punjab */
public record InvoiceResponse(
    /** 业务单头摘要 */
    Header header,
    /** 逐行分配明细 */
    List<Allocation> allocations){
 public InvoiceResponse{allocations=List.copyOf(allocations);}
 /** 来源与状态。 @author Punjab */
 public record Header(
    /** 当前对象ID */
    Long id,
    /** 来源系统标识 */
    String sourceSystem,
    /** 来源单据ID */
    String sourceId,
    /** 外部发票编号 */
    String invoiceNo,
    /** 外部发票日期 */
    LocalDate invoiceDate,
    /** 应收所属客户ID */
    Long customerId,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId,
    /** 结算币种代码 */
    String currency,
    /** 发票方向，蓝字或红字 */
    String invoiceKind,
    /** 红字引用的原蓝字关联ID */
    Long originalInvoiceId,
    /** 带符号的发票分配合计 */
    BigDecimal totalAmount,
    /** 当前业务记录的状态 */
    String status,
    /** 用于并发校验的记录版本 */
    Long version){}
 /** 红字关联保留原分配ID；redAvailableAmount只用于有效蓝字。 @author Punjab */
 public record Allocation(
    /** 当前对象ID */
    Long id,
    /** 外部发票行标识 */
    String invoiceLineId,
    /** 分配所关联的应收行ID */
    Long receivableLineId,
    /** 金额性质，区分价款和销项税 */
    String amountType,
    /** 本记录的业务金额，方向由业务类型决定 */
    BigDecimal amount,
    /** 税率，以小数表示，空值与零税率区分 */
    BigDecimal taxRate,
    /** 红字分配引用的原蓝字分配ID */
    Long originalAllocationId,
    /** 原蓝字分配剩余可红冲金额 */
    BigDecimal redAvailableAmount){}
}
