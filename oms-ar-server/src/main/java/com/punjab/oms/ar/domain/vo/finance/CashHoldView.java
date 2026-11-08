package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;
import java.util.List;
public record CashHoldView(
        /** 冻结单编号。 */ Long id,
        /** 现金账户编号。 */ Long accountId,
        /** 冻结用途，区分人工冻结和提现预留。 */ String purpose,
        /** 冻结单的原始金额。 */ BigDecimal amount,
        /** 已解冻并返还可用余额的累计金额。 */ BigDecimal releasedAmount,
        /** 已从冻结资金中实际支付的累计金额。 */ BigDecimal capturedAmount,
        /** 尚未解冻或支付的金额。 */ BigDecimal remainingAmount,
        /** 按原充值批次拆分的冻结明细。 */ List<Slice> slices){
    public record Slice(
        /** 原充值批次编号。 */ Long lotId,
        /** 该批次在本单中原先冻结的金额。 */ BigDecimal amount,
        /** 已解冻并返还可用余额的累计金额。 */ BigDecimal releasedAmount,
        /** 已从冻结资金中实际支付的累计金额。 */ BigDecimal capturedAmount){}
}
