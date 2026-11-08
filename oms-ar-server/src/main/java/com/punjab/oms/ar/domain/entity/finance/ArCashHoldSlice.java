package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.*;
/** 现金冻结记录。 @author Punjab */
@Getter @Setter @TableName("oms_ar_cash_hold_slice")
public class ArCashHoldSlice extends ArBaseEntity {
    /** 资金归属的现金账户编号。 */
    private Long cashAccountId;
    /** 该充值批次分摊的原始冻结金额。 */
    private BigDecimal amount;
    /** 已解冻并返还可用余额的累计金额。 */
    private BigDecimal releasedAmount;
    /** 已从冻结资金中实际支付的累计金额。 */
    private BigDecimal capturedAmount;
    /** 原冻结单编号。 */
    private Long holdId;
    /** 原充值批次编号。 */
    private Long lotId;
}
