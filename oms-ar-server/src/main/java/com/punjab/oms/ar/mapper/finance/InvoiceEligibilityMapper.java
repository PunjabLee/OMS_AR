package com.punjab.oms.ar.mapper.finance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;import com.punjab.oms.ar.domain.entity.finance.*;import com.punjab.oms.ar.domain.bo.finance.InvoiceEligibilityQueryBO;
import org.apache.ibatis.annotations.*;import java.util.List;
/** 关联负数归原组，独立明细归物理组；不读取核销去向。 @author Punjab */
@Mapper public interface InvoiceEligibilityMapper extends BaseMapper<ArLine> {
 @Select("""
 <script>
 SELECT l.*,COALESCE(a.related_group_id,l.group_id) AS invoice_group_id,d.business_type,e.id AS exchange_basis_id,
 a.original_line_id AS adjustment_original_line_id,o.amount_type AS original_amount_type,o.tax_rate AS original_tax_rate
 FROM oms_ar_line l
 JOIN oms_ar_document d ON d.id=l.document_id AND d.delete_status=false AND d.status='ACTIVE'
 LEFT JOIN oms_ar_adjustment_link a ON a.line_id=l.id AND a.delete_status=false AND l.receivable_amount&lt;0
 LEFT JOIN oms_ar_line o ON o.id=a.original_line_id
 LEFT JOIN oms_ar_exchange_basis e ON e.line_id=l.id AND e.delete_status=false
 WHERE l.delete_status=false AND l.status='ACTIVE'
 AND l.legal_entity_id=#{legalEntityId} AND l.customer_id=#{customerId} AND l.currency=#{currency}
 AND COALESCE(a.related_group_id,l.group_id) IN
 <foreach collection="groupIds" item="id" open="(" separator="," close=")">#{id}</foreach>
 ORDER BY l.id
 </script>
 """)
 List<ArInvoiceEligibilityLine> lines(InvoiceEligibilityQueryBO query);
}
