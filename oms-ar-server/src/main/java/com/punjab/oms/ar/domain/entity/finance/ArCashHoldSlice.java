package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.*;
/** 现金冻结记录。 @author Punjab */
@Getter @Setter @TableName("oms_ar_cash_hold_slice")
public class ArCashHoldSlice extends ArBaseEntity {
    private Long cashAccountId;
    private BigDecimal amount;
    private BigDecimal releasedAmount;
    private BigDecimal capturedAmount;
    private Long holdId;
    private Long lotId;
}
