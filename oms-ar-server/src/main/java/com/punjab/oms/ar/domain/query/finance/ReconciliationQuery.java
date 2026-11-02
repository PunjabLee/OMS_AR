package com.punjab.oms.ar.domain.query.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;import jakarta.validation.constraints.*;
/** 必选完整结算范围的只读对账请求。 @author Punjab */
@Getter @Setter public class ReconciliationQuery extends QueryPage {@NotNull @Positive private Long legalEntityId;@NotNull @Positive private Long customerId;@NotBlank @Pattern(regexp="[A-Z]{3}") private String currency;}
