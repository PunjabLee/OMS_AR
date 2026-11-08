package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;

/** 提现详情仅展示受托账户尾号。 @author Punjab */
public record WithdrawalResponse(
        /** 提现申请编号。 */ Long id,
        /** 资金归属的现金账户编号。 */ Long cashAccountId,
        /** 接收提现款项的受托账户编号。 */ Long payoutAccountId,
        /** 仅显示尾号的受托收款账号。 */ String maskedAccount,
        /** 结算币种代码。 */ String currency,
        /** 申请提现的金额。 */ BigDecimal amount,
        /** 提现申请当前的处理状态。 */ String status,
        /** 提现申请人。 */ String applicant,
        /** 提现审核人。 */ String reviewer,
        /** 已认领线下付款的操作人。 */ String claimedBy,
        /** 支付成功后登记的银行回单号。 */ String bankReceipt,
        /** 线下核实付款结果的凭证。 */ String evidence,
        /** 并发控制版本。 */ Long version) {}
