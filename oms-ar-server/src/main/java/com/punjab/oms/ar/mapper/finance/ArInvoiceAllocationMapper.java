package com.punjab.oms.ar.mapper.finance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;import com.punjab.oms.ar.domain.entity.finance.ArInvoiceAllocation;import org.apache.ibatis.annotations.Mapper;
/** 发票关联映射。 @author Punjab */
@Mapper public interface ArInvoiceAllocationMapper extends BaseMapper<ArInvoiceAllocation>{
 @org.apache.ibatis.annotations.Select("SELECT a.* FROM oms_ar_invoice_allocation a JOIN oms_ar_invoice i ON i.id=a.invoice_id WHERE a.original_invoice_id=#{id} AND a.delete_status=false AND i.delete_status=false AND i.status='ACTIVE' ORDER BY a.id")
 java.util.List<ArInvoiceAllocation> activeRedAllocations(Long id);
}
