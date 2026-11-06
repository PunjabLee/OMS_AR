package com.punjab.oms.ar.domain.query.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;import jakarta.validation.constraints.*;
/** 财务分页筛选范围。 @author Punjab */
@Getter @Setter public class AccountPageQuery extends QueryPage {

    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive private Long legalEntityId;

    /** 应收所属客户ID */
    @Positive private Long customerId;

    /** 结算币种代码 */
    @Pattern(regexp="[A-Z]{3}") private String currency;
}
