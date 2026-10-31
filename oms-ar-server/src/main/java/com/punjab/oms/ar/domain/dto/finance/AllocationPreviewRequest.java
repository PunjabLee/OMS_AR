package com.punjab.oms.ar.domain.dto.finance;
import com.punjab.oms.ar.enums.*;
import java.math.BigDecimal;
import java.util.List;
/** 由调用服务从可信余额构造，组和行的列表顺序即用户确认顺序。 @author Punjab */
public record AllocationPreviewRequest(BigDecimal availableAmount, int currencyScale,
        AllocationStrategy strategy, List<Group> groups) {
    /** 同一业务组的正数目标行。 @author Punjab */
    public record Group(Long groupId, List<Line> lines) {}
    /** 当前可核销余额；版本在最终确认时由持久化服务校验。 @author Punjab */
    public record Line(Long lineId, AmountType amountType, BigDecimal openAmount) {}
}
