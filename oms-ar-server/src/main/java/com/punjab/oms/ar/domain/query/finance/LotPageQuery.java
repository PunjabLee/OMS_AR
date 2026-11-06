package com.punjab.oms.ar.domain.query.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;import jakarta.validation.constraints.*;
/** 财务分页筛选范围。 @author Punjab */
@Getter @Setter public class LotPageQuery extends QueryPage {

    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive private Long legalEntityId;

    /** 应收所属客户ID */
    @Positive private Long customerId;

    /** 结算币种代码 */
    @Pattern(regexp="[A-Z]{3}") private String currency;

    /** 当前业务记录的状态 */
    @Pattern(regexp="ACTIVE|REVERSED") private String status;

    /** 客户信用账户ID */
    @Positive private Long accountId;

    /** 可选的来源行标识 */
    @Positive private Long sourceLineId;
}
