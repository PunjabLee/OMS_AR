package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.*;
/** 现金冻结记录。 @author Punjab */
@Getter @Setter @TableName("oms_ar_cash_hold")
public class ArCashHold extends ArBaseEntity {
    /** 资金归属的现金账户编号。 */
    private Long cashAccountId;
    /** 冻结单原始金额。 */
    private BigDecimal amount;
    /** 已解冻并返还可用余额的累计金额。 */
    private BigDecimal releasedAmount;
    /** 已从冻结资金中实际支付的累计金额。 */
    private BigDecimal capturedAmount;
    /** 冻结用途，区分人工冻结和提现预留。 */
    private String purpose;
    /** 并发控制版本。 */
    private Long version;
}
