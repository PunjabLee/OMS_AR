package com.punjab.oms.ar.domain.dto.finance;
import com.punjab.oms.ar.enums.*;
import java.math.BigDecimal;
import java.util.List;
/** 由调用服务从可信余额构造，组和行的列表顺序即用户确认顺序。 @author Punjab */
public record AllocationPreviewRequest(
    /** 可用于分配的金额 */
    BigDecimal availableAmount,
    /** 结算币种允许的小数位数 */
    int currencyScale,
        /** 用户选定的核销分配策略 */
        AllocationStrategy strategy,
    /** 参与本次业务的组集合 */
    List<Group> groups) {
    /** 同一业务组的正数目标行。 @author Punjab */
    public record Group(
    /** 业务组ID */
    Long groupId,
    /** 本次请求或结果涉及的明细 */
    List<Line> lines) {}
    /** 当前可核销余额；版本在最终确认时由持久化服务校验。 @author Punjab */
    public record Line(
    /** 业务明细行标识 */
    Long lineId,
    /** 金额性质，区分价款和销项税 */
    AmountType amountType,
    /** 当前未核销、未转出的应收余额 */
    BigDecimal openAmount) {}
}
