package com.punjab.oms.ar.domain.vo.finance;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 现金账户变动。 @author Punjab */
public record CashEntryResponse(
    /** 变更明细。 */ Long changeId,
    /** 业务事件。 */ Long eventId,
    /** 事件类型。 */ String eventType,
    /** 发生时间。 */ LocalDateTime occurredAt,
    /** 操作人。 */ String actor,
    /** 操作事由。 */ String reason,
    /** 变更前账面额。 */ BigDecimal bookBefore,
    /** 变更后账面额。 */ BigDecimal bookAfter,
    /** 变更前冻结额。 */ BigDecimal frozenBefore,
    /** 变更后冻结额。 */ BigDecimal frozenAfter) {}
