package com.punjab.oms.ar.domain.query.finance;
import jakarta.validation.constraints.*;import java.util.List;
/** 显式选择同一客户/币种的业务组查询开票口径。 @author Punjab */
public record InvoiceEligibilityQuery(@NotNull @Positive Long legalEntityId,@NotNull @Positive Long customerId,
 @NotBlank @Size(min=3,max=3) String currency,@NotEmpty @Size(max=200) List<@NotNull @Positive Long> groupIds){}
