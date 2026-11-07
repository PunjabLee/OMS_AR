package com.punjab.oms.ar.domain.vo.finance;

import java.math.BigDecimal;
import java.util.List;

/** 现金账户核销预览，列出应收目标与实际出资批次。 @author Punjab */
public record CashSettlementView(
        /** 用户确认的数据指纹。 */ String fingerprint,
        /** 实际使用的资金。 */ BigDecimal usedAmount,
        /** 超出目标应收的剩余金额。 */ BigDecimal unallocatedAmount,
        /** 按组先税后价的目标。 */ List<AllocationPreviewResponse.Allocation> allocations,
        /** 按批次顺序列出资金来源。 */ List<Funding> funding) {
    /** 单个充值批次供应的资金。 @author Punjab */
    public record Funding(Long lotId, Long receiptId, BigDecimal amount) {}
    /** 每个来源收款对应的原始核销单，可按原单反核销。 @author Punjab */
    public record Posted(Long eventId, List<Long> settlementIds) {}
}
