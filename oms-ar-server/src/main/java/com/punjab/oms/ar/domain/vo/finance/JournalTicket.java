package com.punjab.oms.ar.domain.vo.finance;
/** 当前事务内的幂等占位。 @author Punjab */
public record JournalTicket(
    /** 当前对象ID */
    Long id,
    /** 操作已完成时可重放的结果引用 */
    Long resultReference,
    /** 执行动作的可信操作人 */
    String actor,
    /** 核算主体ID，也是权限范围 */
    Long legalEntityId){}
