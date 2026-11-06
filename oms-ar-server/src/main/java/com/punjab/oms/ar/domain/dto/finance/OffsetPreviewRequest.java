package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;import java.util.List;import java.math.BigDecimal;
import com.punjab.oms.ar.enums.AllocationStrategy;
/** 负数抵销预览；有原关联时由服务端决定同类型限制。 @author Punjab */
public record OffsetPreviewRequest(
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
    /** 提供抵销额度的负数应收行ID */
    @NotNull @Positive Long negativeLineId,
 /** 本记录的业务金额，方向由业务类型决定 */
 @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=16,fraction=8) BigDecimal amount,
 /** 选定的业务组ID集合 */
 @NotNull @Size(max=200) List<@NotNull @Positive Long> groupIds,
    /** 是否经用户确认继续寻找其他同类型目标 */
    boolean continueOtherTargets,
    /** 用户选定的核销分配策略 */
    @NotNull AllocationStrategy strategy){}
