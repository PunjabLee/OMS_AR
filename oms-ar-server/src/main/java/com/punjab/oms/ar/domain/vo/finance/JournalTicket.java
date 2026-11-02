package com.punjab.oms.ar.domain.vo.finance;
/** 当前事务内的幂等占位。 @author Punjab */
public record JournalTicket(Long id,Long resultReference,String actor,Long legalEntityId){}
