package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;import java.util.List;
/** 只读额度，行剩余额不可越过同组/类型/税率共享额度叠加使用。 @author Punjab */
public record InvoiceEligibilityResponse(
    /** 按原组、金额类型和税率归集的额度 */
    List<Bucket> buckets,
    /** 本次请求或结果涉及的明细 */
    List<Line> lines,
    /** 当前预览所依赖数据的摘要 */
    String fingerprint){
 public InvoiceEligibilityResponse{buckets=List.copyOf(buckets);lines=List.copyOf(lines);}
 /** 正负应收及已开票均带原方向，负的remainingNetAmount不等于可红字额度。 @author Punjab */
 public record Bucket(
    /** 业务组ID */
    Long groupId,
    /** 金额性质，区分价款和销项税 */
    String amountType,
    /** 税率，以小数表示，空值与零税率区分 */
    BigDecimal taxRate,
    /** 该额度组带符号的应收合计 */
    BigDecimal receivableNetAmount,
    /** 该额度组带符号的已开票合计 */
    BigDecimal invoicedNetAmount,
    /** 共享额度扣除开票后的带符号净额 */
    BigDecimal remainingNetAmount,
    /** 当前共享额度允许的蓝字金额 */
    BigDecimal blueAvailableAmount){}
 /** 单行额度仅提供自身上限，实际蓝字还需共享bucket限制；红字还需原发票分配限制。 @author Punjab */
 public record Line(
    /** 当前对象ID */
    Long id,
    /** 应收行实际所属的业务组ID */
    Long physicalGroupId,
    /** 计算共享开票额度所用的业务组ID */
    Long invoiceGroupId,
    /** 金额性质，区分价款和销项税 */
    String amountType,
    /** 税率，以小数表示，空值与零税率区分 */
    BigDecimal taxRate,
    /** 带符号的原始应收金额 */
    BigDecimal receivableAmount,
    /** 本行已登记的带符号开票金额 */
    BigDecimal invoicedAmount,
    /** 本行剩余可关联金额 */
    BigDecimal remainingAmount,
    /** 用于并发校验的记录版本 */
    Long version,
    /** 当前明细是否具有开票资格 */
    boolean invoiceSupported,
    /** 不满足业务资格的原因 */
    String blockedReason){}
}
