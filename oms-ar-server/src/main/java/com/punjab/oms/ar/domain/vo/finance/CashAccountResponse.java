package com.punjab.oms.ar.domain.vo.finance;

import java.math.BigDecimal;

/** 现金账户余额详情。 @author Punjab */
public record CashAccountResponse(
    /** 账户ID。 */ Long id, /** 主体ID。 */ Long legalEntityId,
    /** 客户ID。 */ Long customerId, /** 币种。 */ String currency,
    /** 开票属性。 */ String invoiceMode, /** 状态。 */ String status,
    /** 账面余额。 */ BigDecimal bookAmount, /** 冻结额。 */ BigDecimal frozenAmount,
    /** 可用资金。 */ BigDecimal availableAmount, /** 版本。 */ Long version) {}
