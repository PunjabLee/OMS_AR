package com.punjab.oms.ar.domain.query.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;import jakarta.validation.constraints.*;
/** 必选完整结算范围的只读对账请求。 @author Punjab */
@Getter @Setter public class ReconciliationQuery extends QueryPage {
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive private Long legalEntityId;
    /** 应收所属客户ID */
    @NotNull @Positive private Long customerId;
    /** 结算币种代码 */
    @NotBlank @Pattern(regexp="[A-Z]{3}") private String currency;}
