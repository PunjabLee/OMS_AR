package com.punjab.oms.ar.domain.query.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;import jakarta.validation.constraints.*;
/** 通知运营筛选，主体必填。 @author Punjab */
@Getter @Setter public class OutboxPageQuery extends QueryPage {
 /** 核算主体ID，也是权限范围 */
 @NotNull @Positive private Long legalEntityId;
    /** 事件标识 */
    @Positive private Long eventId;
 /** 当前业务记录的状态 */
 @Pattern(regexp="PENDING|PROCESSING|RETRY|SENT|DEAD") private String status;
 /** 通知的逻辑投递目的地 */
 @Pattern(regexp="FINANCE_EVENTS") private String destination;
}
