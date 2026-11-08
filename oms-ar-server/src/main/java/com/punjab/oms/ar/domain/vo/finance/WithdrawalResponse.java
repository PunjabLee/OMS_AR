package com.punjab.oms.ar.domain.vo.finance;
import java.math.BigDecimal;

/** 提现详情仅展示受托账户尾号。 @author Punjab */
public record WithdrawalResponse(Long id,Long cashAccountId,Long payoutAccountId,String maskedAccount,
        String currency,BigDecimal amount,String status,String applicant,String reviewer,String claimedBy,
        String bankReceipt,String evidence,Long version) {}
