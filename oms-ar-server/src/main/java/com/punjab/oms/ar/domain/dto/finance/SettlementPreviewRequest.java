package com.punjab.oms.ar.domain.dto.finance;
import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.util.List;
import com.punjab.oms.ar.enums.AllocationStrategy;
/** 明确选中业务组的全部有效正数应收，组顺序由用户确认。 @author Punjab */
public record SettlementPreviewRequest(@NotNull @Positive Long legalEntityId,@NotNull @Positive Long receiptId,
 @NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=16,fraction=8) BigDecimal amount,
 @NotEmpty @Size(max=200) List<@NotNull @Positive Long> groupIds,@NotNull AllocationStrategy strategy){}
