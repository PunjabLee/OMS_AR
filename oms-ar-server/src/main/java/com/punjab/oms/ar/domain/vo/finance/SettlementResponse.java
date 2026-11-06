package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;import java.time.LocalDateTime;import java.util.List;
/** 原分配和来源批次，反核销后仍保留。 @author Punjab */
public record SettlementResponse(
    /** 业务单头摘要 */
    Header header,
    /** 客户信用账户ID */
    Long accountId,
    /** 逐行分配明细 */
    List<Allocation> allocations,
    /** 核销分配对应的批次使用明细 */
    List<Usage> usages){
 public SettlementResponse{allocations=List.copyOf(allocations);usages=List.copyOf(usages);}
 /** 核销摘要。 @author Punjab */
 public record Header(
    /** 当前对象ID */
    Long id,
    /** 核销单编号 */
    String settlementNo,
    /** 应收所属客户ID */
    Long customerId,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId,
    /** 结算币种代码 */
    String currency,
    /** 核销资金类型，区分实收、负数抵销和余额抵扣 */
    String settlementType,
    /** 实际收款记录ID */
    Long receiptId,
    /** 本记录的业务金额，方向由业务类型决定 */
    BigDecimal amount,
    /** 用户选定的核销分配策略 */
    String strategy,
    /** 当前业务记录的状态 */
    String status,
    /** 业务操作原因 */
    String reason,
    /** 用于并发校验的记录版本 */
    Long version,
    /** 记录创建时间 */
    LocalDateTime createdTime,
    /** 创建记录的操作人 */
    String createdBy){}
 /** 双方应收及原始分配金额。 @author Punjab */
 public record Allocation(
    /** 当前对象ID */
    Long id,
    /** 分配或使用明细的顺序号 */
    Integer sequenceNo,
    /** 本次分配抵扣的正数应收行ID */
    Long positiveLineId,
    /** 提供抵销额度的负数应收行ID */
    Long negativeLineId,
    /** 金额性质，区分价款和销项税 */
    String amountType,
    /** 本记录的业务金额，方向由业务类型决定 */
    BigDecimal amount){}
 /** 一笔分配可能使用多个批次。 @author Punjab */
 public record Usage(
    /** 当前对象ID */
    Long id,
    /** 业务分配记录ID */
    Long allocationId,
    /** 信用余额来源批次ID */
    Long lotId,
    /** 分配或使用明细的顺序号 */
    Integer sequenceNo,
    /** 本记录的业务金额，方向由业务类型决定 */
    BigDecimal amount){}
}
