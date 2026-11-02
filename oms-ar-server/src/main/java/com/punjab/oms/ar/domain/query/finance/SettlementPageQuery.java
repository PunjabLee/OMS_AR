package com.punjab.oms.ar.domain.query.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;import jakarta.validation.constraints.*;
/** 财务分页筛选范围。 @author Punjab */
@Getter @Setter public class SettlementPageQuery extends QueryPage {
@NotNull @Positive private Long legalEntityId;
@Positive private Long customerId;
@Pattern(regexp="[A-Z]{3}") private String currency;
@Pattern(regexp="POSTED|REVERSED") private String status;
@Pattern(regexp="RECEIPT|OFFSET|BALANCE") private String settlementType;
@Positive private Long receiptId;
}
