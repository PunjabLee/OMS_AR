package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.InvoiceEligibilityQueryBO;import java.util.List;
/** 开票归属及范围一致性查询。 @author Punjab */
public interface InvoiceEligibilityDao {List<ArGroup> groups(InvoiceEligibilityQueryBO query);List<ArInvoiceEligibilityLine> lines(InvoiceEligibilityQueryBO query);}
