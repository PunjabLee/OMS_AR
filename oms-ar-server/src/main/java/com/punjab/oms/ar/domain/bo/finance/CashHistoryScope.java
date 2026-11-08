package com.punjab.oms.ar.domain.bo.finance;

/** 已验证的主体、账户与分页偏移。 @author Punjab */
public record CashHistoryScope(
        /** 现金账户所属核算主体编号。 */ Long legalEntityId,
        /** 现金账户编号。 */ Long accountId,
        /** 查询结果的起始偏移量，从零开始。 */ long offset,
        /** 每页最多返回的记录数。 */ int size) {}
