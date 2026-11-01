package com.punjab.oms.ar.domain.bo.finance;
import java.util.List;
/** 开票归属范围，Query经转换后进入DAO。 @author Punjab */
public record InvoiceEligibilityQueryBO(Long legalEntityId,Long customerId,String currency,List<Long> groupIds){}
