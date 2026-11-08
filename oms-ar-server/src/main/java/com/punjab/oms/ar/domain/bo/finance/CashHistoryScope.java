package com.punjab.oms.ar.domain.bo.finance;

/** 已验证的主体、账户与分页偏移。 @author Punjab */
public record CashHistoryScope(Long legalEntityId, Long accountId, long offset, int size) {}
