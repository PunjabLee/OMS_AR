package com.punjab.oms.ar.domain.bo.finance;
import com.punjab.oms.ar.base.QueryPage;import lombok.Getter;import lombok.Setter;
/** 通知持久层查询条件。 @author Punjab */
@Getter @Setter public class OutboxPageQueryBO extends QueryPage {
    /** 核算主体ID，也是权限范围 */
    private Long legalEntityId;
    /** 事件标识 */
    private Long eventId;
    /** 当前业务记录的状态 */
    private String status;
    /** 通知的逻辑投递目的地 */
    private String destination;}
