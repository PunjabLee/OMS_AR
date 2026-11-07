package com.punjab.oms.ar.domain.entity.finance;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

/** 每笔实际到账唯一对应一个资金批次，已核销金额由原收款维护。 @author Punjab */
@Getter @Setter @TableName("oms_ar_cash_receipt_lot")
public class ArCashReceiptLot extends ArBaseEntity {
    /** 原收款单。 */
    private Long receiptId;
    /** 资金账户。 */
    private Long cashAccountId;
    /** 客户。 */
    private Long customerId;
    /** 核算主体。 */
    private Long legalEntityId;
    /** 结算币种。 */
    private String currency;
    /** 批次冻结额。 */
    private BigDecimal frozenAmount;
    /** 已确认提现金额。 */
    private BigDecimal withdrawnAmount;
}
