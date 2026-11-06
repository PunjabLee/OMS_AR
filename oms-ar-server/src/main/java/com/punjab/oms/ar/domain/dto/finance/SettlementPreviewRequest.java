package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.util.List;
import com.punjab.oms.ar.enums.AllocationStrategy;
/** 明确选中业务组的全部有效正数应收，组顺序由用户确认。 @author Punjab */
public record SettlementPreviewRequest(
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
    /** 实际收款记录ID */
    @NotNull @Positive Long receiptId,
 /** 本记录的业务金额，方向由业务类型决定 */
 @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=16,fraction=8) BigDecimal amount,
 /** 选定的业务组ID集合 */
 @NotEmpty @Size(max=200) List<@NotNull @Positive Long> groupIds,
    /** 用户选定的核销分配策略 */
    @NotNull AllocationStrategy strategy){}
