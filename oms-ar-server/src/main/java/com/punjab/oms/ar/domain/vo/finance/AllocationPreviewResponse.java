package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;
import java.util.List;
import com.punjab.oms.ar.enums.AmountType;
/** 分配预览，不占用资金。 @author Punjab */
public record AllocationPreviewResponse(
    /** 逐行分配明细 */
    List<Allocation> allocations,
    /** 本次请求金额中未分配的部分 */
    BigDecimal unallocatedAmount) {
    public AllocationPreviewResponse { allocations = List.copyOf(allocations); }
    /** 仅记录正数实际分配。 @author Punjab */
    public record Allocation(
    /** 业务组ID */
    Long groupId,
    /** 业务明细行标识 */
    Long lineId,
    /** 金额性质，区分价款和销项税 */
    AmountType amountType,
    /** 本记录的业务金额，方向由业务类型决定 */
    BigDecimal amount) {}
}
