package com.punjab.oms.ar.domain.bo.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;
/** 重试记录分页内部条件。 @author Punjab */
@Getter @Setter public class OutboxRetryPageQueryBO extends QueryPage {
    /** 核算主体ID，也是权限范围 */
    private Long legalEntityId;
    /** 待发送通知记录ID */
    private Long outboxId;}
