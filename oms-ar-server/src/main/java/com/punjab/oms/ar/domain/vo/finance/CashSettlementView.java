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
    public record Funding(
        /** 原充值批次编号。 */ Long lotId,
        /** 提供本次核销资金的原收款单编号。 */ Long receiptId,
        /** 该充值批次承担的核销金额。 */ BigDecimal amount) {}
    /** 每个来源收款对应的原始核销单，可按原单反核销。 @author Punjab */
    public record Posted(
        /** 本次账户核销对应的业务事件编号。 */ Long eventId,
        /** 各出资收款单生成的核销单编号。 */ List<Long> settlementIds) {}
}
