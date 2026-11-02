package com.punjab.oms.ar.domain.query.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;import jakarta.validation.constraints.*;
/** 通知运营筛选，主体必填。 @author Punjab */
@Getter @Setter public class OutboxPageQuery extends QueryPage {
 @NotNull @Positive private Long legalEntityId;@Positive private Long eventId;
 @Pattern(regexp="PENDING|PROCESSING|RETRY|SENT|DEAD") private String status;
 @Pattern(regexp="FINANCE_EVENTS") private String destination;
}
