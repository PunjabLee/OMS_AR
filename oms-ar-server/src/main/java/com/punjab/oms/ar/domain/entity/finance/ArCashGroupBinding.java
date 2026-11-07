package com.punjab.oms.ar.domain.entity.finance;

import lombok.Getter;
import lombok.Setter;
import com.baomidou.mybatisplus.annotation.TableName;

/** 应收业务组的现金账户归属，同组各行共同继承。 @author Punjab */
@Getter @Setter @TableName("oms_ar_cash_group_binding")
public class ArCashGroupBinding extends ArBaseEntity {
    /** 业务组ID。 */
    private Long groupId;
    /** 唯一归属的现金账户ID。 */
    private Long cashAccountId;
    /** 客户ID。 */
    private Long customerId;
    /** 核算主体ID。 */
    private Long legalEntityId;
    /** 结算币种。 */
    private String currency;
}
