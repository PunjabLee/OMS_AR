package com.punjab.oms.ar.domain.vo.finance;
import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDate;
/** 应收详情。 @author Punjab */
public record ReceivableResponse(
    /** 应收单头信息 */
    Document document,
    /** 按有效明细余额计算的结算程度 */
    String settlementStatus,
    /** 本次请求或结果涉及的明细 */
    List<Line> lines,
    /** 不含税价款分类金额汇总 */
    Amounts principal,
    /** 销项税分类金额汇总 */
    Amounts outputTax){
 public ReceivableResponse{lines=List.copyOf(lines);}
 /** 单据摘要。 @author Punjab */
 public record Document(
    /** 当前对象ID */
    Long id,
    /** 应收单编号 */
    String docNo,
    /** 应收所属客户ID */
    Long customerId,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId,
    /** 结算币种代码 */
    String currency,
    /** 来源业务类别，例如出库或汇率差异 */
    String businessType,
    /** 当前业务记录的状态 */
    String status,
    /** 业务发生日期 */
    LocalDate businessDate,
    /** 应收到期日期 */
    LocalDate dueDate,
    /** 用于并发校验的记录版本 */
    Long version,
    /** 生成方式，区分直接录入与上游生成 */
    String generationMode,
    /** 财务或上游作废原因 */
    String voidReason,
    /** 执行整单作废的操作人 */
    String voidedBy,
    /** 整单作废完成时间 */
    java.time.LocalDateTime voidedTime){}
 /** 明细。 @author Punjab */
 public record Line(
    /** 当前对象ID */
    Long id,
    /** 业务组ID */
    Long groupId,
    /** 应收单内的明细行序号 */
    Integer lineNo,
    /** 金额性质，区分价款和销项税 */
    String amountType,
    /** 当前业务记录的状态 */
    String status,
    /** 带符号的原始应收金额 */
    BigDecimal receivableAmount,
    /** 已核销金额，沿用原应收方向 */
    BigDecimal settledAmount,
    /** 从负数应收转入信用账户的带符号金额 */
    BigDecimal transferredAmount,
    /** 当前未核销、未转出的应收余额 */
    BigDecimal openAmount,
    /** 本行已登记的带符号开票金额 */
    BigDecimal invoicedAmount,
    /** 税率，以小数表示，空值与零税率区分 */
    BigDecimal taxRate,
    /** 上游确认的计税依据金额 */
    BigDecimal taxBaseAmount,
    /** 生成明细的来源信息 */
    Source source,
    /** 用于并发校验的记录版本 */
    Long version,
    /** 原币金额和汇率折算依据 */
    ExchangeBasis exchangeBasis,
    /** 负数应收的原组与原行关联 */
    Adjustment adjustment,
    /** 分户范围内的现金账户ID。 */
    Long cashAccountId,
    /** 所属现金账户开票属性。 */
    String invoiceMode){}
 /** 独立于来源单据的调整关系。 @author Punjab */
 public record Adjustment(
    /** 负数调整归属的原业务组ID */
    Long relatedGroupId,
    /** 调整所指定的原正数应收行ID */
    Long originalLineId){}
 /** 来源。 @author Punjab */
 public record Source(
    /** 来源系统标识 */
    String sourceSystem,
    /** 来源单据类型 */
    String sourceType,
    /** 来源单据编号 */
    String sourceNo,
    /** 来源单据ID */
    String sourceId,
    /** 可选的来源行标识 */
    String sourceLineId,
    /** 来源系统的事件标识 */
    String sourceEventId,
    /** 同一来源事件内的输出标识 */
    String sourceOutputKey,
    /** 调整所指定的原正数应收行ID */
    Long originalLineId){}
 /** 汇率计算快照。 @author Punjab */
 public record ExchangeBasis(
    /** 折算前的原币种 */
    String originalCurrency,
    /** 原始金额 */
    BigDecimal originalAmount,
    /** 原换算汇率 */
    BigDecimal oldRate,
    /** 新的换算汇率 */
    BigDecimal newRate,
    /** 汇率报价方向，决定乘法或除法折算 */
    String rateDirection,
    /** 按原汇率计算的结算币金额 */
    BigDecimal oldSettlementAmount,
    /** 按新汇率计算的结算币金额 */
    BigDecimal newSettlementAmount,
    /** 以结算币种确认的汇率差额 */
    BigDecimal confirmedDifference){}
 /** 相同金额类型的各口径。 @author Punjab */
 public record Amounts(
    /** 带符号的原始应收金额 */
    BigDecimal receivableAmount,
    /** 已核销金额，沿用原应收方向 */
    BigDecimal settledAmount,
    /** 从负数应收转入信用账户的带符号金额 */
    BigDecimal transferredAmount,
    /** 当前未核销、未转出的应收余额 */
    BigDecimal openAmount,
    /** 本行已登记的带符号开票金额 */
    BigDecimal invoicedAmount){}
}
