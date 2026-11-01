package com.punjab.oms.ar.service.finance;
import com.punjab.oms.ar.domain.query.finance.InvoiceEligibilityQuery;import com.punjab.oms.ar.domain.vo.finance.InvoiceEligibilityResponse;
/** 开票额度只读查询，不占用额度或实际开票。 @author Punjab */
public interface InvoiceEligibilityService {InvoiceEligibilityResponse query(InvoiceEligibilityQuery query);}
