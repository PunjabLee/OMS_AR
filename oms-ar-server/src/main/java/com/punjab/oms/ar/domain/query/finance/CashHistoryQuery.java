package com.punjab.oms.ar.domain.query.finance;

import com.punjab.oms.ar.base.QueryPage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** 按主体和账户读取充值批次或资金流水。 @author Punjab */
@Getter @Setter
public class CashHistoryQuery extends QueryPage {
    /** 核算主体。 */
    @NotNull @Positive private Long legalEntityId;
    /** 现金账户。 */
    @NotNull @Positive private Long accountId;
}
