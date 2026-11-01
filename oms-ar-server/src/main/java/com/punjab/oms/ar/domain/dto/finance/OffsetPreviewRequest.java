package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;import java.util.List;import java.math.BigDecimal;
import com.punjab.oms.ar.enums.AllocationStrategy;
/** 负数抵销预览；有原关联时由服务端决定同类型限制。 @author Punjab */
public record OffsetPreviewRequest(@NotNull @Positive Long legalEntityId,@NotNull @Positive Long negativeLineId,
 @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=16,fraction=8) BigDecimal amount,
 @NotNull @Size(max=200) List<@NotNull @Positive Long> groupIds,boolean continueOtherTargets,@NotNull AllocationStrategy strategy){}
