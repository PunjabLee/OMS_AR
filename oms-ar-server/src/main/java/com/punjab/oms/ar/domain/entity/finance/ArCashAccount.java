package com.punjab.oms.ar.domain.entity.finance;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

/** 按主体、客户、币种及开票属性隔离的现金账户。 @author Punjab */
@Getter @Setter @TableName("oms_ar_cash_account")
public class ArCashAccount extends ArBaseEntity {
    /** 资金所属客户。 */
    private Long customerId;
    /** 核算主体。 */
    private Long legalEntityId;
    /** 账户币种。 */
    private String currency;
    /** 开票用途。 */
    private String invoiceMode;
    /** 账户状态。 */
    private String status;
    /** 已入账现金余额。 */
    private BigDecimal bookAmount;
    /** 已冻结资金总额。 */
    private BigDecimal frozenAmount;
    /** 数据库根据账面与冻结额计算的可用余额。 */
    @TableField(insertStrategy=FieldStrategy.NEVER, updateStrategy=FieldStrategy.NEVER)
    private BigDecimal availableAmount;
    /** 并发版本。 */
    private Long version;
}
