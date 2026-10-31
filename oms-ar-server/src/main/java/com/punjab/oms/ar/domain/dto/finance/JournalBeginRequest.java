package com.punjab.oms.ar.domain.dto.finance;
/** 由业务服务构造，不作为HTTP契约。 @author Punjab */
public record JournalBeginRequest(Long legalEntityId,String permission,String action,String requestKey,String payload){}
