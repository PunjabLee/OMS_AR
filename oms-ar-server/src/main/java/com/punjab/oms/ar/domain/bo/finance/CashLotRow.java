package com.punjab.oms.ar.domain.bo.finance;

import java.math.BigDecimal;

/** 现金批次。 @author Punjab */
public record CashLotRow(
    /** 充值批次。 */ Long lotId,
    /** 原收款。 */ Long receiptId,
    /** 来源单号。 */ String sourceNo,
    /** 到账原额。 */ BigDecimal amount,
    /** 已核销额。 */ BigDecimal allocated,
    /** 冻结金额。 */ BigDecimal frozen,
    /** 已提现额。 */ BigDecimal withdrawn,
    /** 可用额。 */ BigDecimal available) {}
