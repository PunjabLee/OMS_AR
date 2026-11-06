package com.punjab.oms.ar.domain.query.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;import jakarta.validation.constraints.*;
/** 单条通知的人工重试历史。 @author Punjab */
@Getter @Setter public class OutboxRetryPageQuery extends QueryPage {
    /** 核算主体ID，也是权限范围 */
    @NotNull @Positive private Long legalEntityId;
    /** 待发送通知记录ID */
    @NotNull @Positive private Long outboxId;}
