package com.punjab.oms.ar.domain.entity.finance;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import lombok.*;
/** 现金冻结记录。 @author Punjab */
@Getter @Setter @TableName("oms_ar_cash_hold")
public class ArCashHold extends ArBaseEntity {
    private Long cashAccountId;
    private BigDecimal amount;
    private BigDecimal releasedAmount;
    private BigDecimal capturedAmount;
    private String purpose;
    private Long version;
}
