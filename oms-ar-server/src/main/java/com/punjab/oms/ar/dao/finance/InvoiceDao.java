package com.punjab.oms.ar.dao.finance;
import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.InvoiceDetailQueryBO;import java.util.List;
/** 发票与应收开票口径聚合访问，不在DAO开启事务。 @author Punjab */
public interface InvoiceDao {
 ArInvoice invoice(Long id,Long scope,boolean lock);ArInvoice detail(InvoiceDetailQueryBO query);
 List<ArInvoiceAllocation> allocations(Long invoiceId);List<ArInvoiceAllocation> activeRedAllocations(Long invoiceId);
 List<ArInvoiceEligibilityLine> sources(List<Long> ids,Long scope);List<ArGroup> lockGroups(List<Long> ids,Long scope);
 boolean create(ArInvoice value);boolean allocate(ArInvoiceAllocation value);boolean update(ArInvoice value,Long expected);boolean updateLine(ArLine value,Long expected);
 long activeRedCount(Long id);Long postedEvent(Long id);
}
