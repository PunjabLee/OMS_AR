package com.punjab.oms.ar.domain.query.finance;
import jakarta.validation.constraints.*;import java.util.List;
/** 显式选择同一客户/币种的业务组查询开票口径。 @author Punjab */
public record InvoiceEligibilityQuery(
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive Long legalEntityId,
    /** 应收所属客户ID */
    @NotNull @Positive Long customerId,
 /** 结算币种代码 */
 @NotBlank @Size(min=3,max=3) String currency,
    /** 选定的业务组ID集合 */
    @NotEmpty @Size(max=200) List<@NotNull @Positive Long> groupIds){}
