package com.punjab.oms.ar.mapper.finance;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;import com.punjab.oms.ar.domain.entity.finance.*;import org.apache.ibatis.annotations.*;import java.util.List;
/** 作废依赖及来源匹配查询，事务锁由DAO明确管理。 @author Punjab */
@Mapper public interface ReceivableVoidMapper extends BaseMapper<ArLine> {
 @Select("SELECT l.* FROM oms_ar_line l JOIN oms_ar_line_source s ON s.line_id=l.id AND s.delete_status=false WHERE l.delete_status=false AND l.legal_entity_id=#{legalEntityId} AND s.source_system=#{sourceSystem} AND s.source_type=#{sourceType} AND s.source_id=#{sourceId} AND s.source_line_id <=> #{sourceLineId} ORDER BY l.id")
 List<ArLine> sourceLines(ArSourceGuard source);
 @Select("SELECT COUNT(*) FROM oms_ar_settlement_allocation a JOIN oms_ar_settlement h ON h.id=a.settlement_id AND h.status='POSTED' AND h.delete_status=false WHERE a.delete_status=false AND (a.positive_line_id=#{id} OR a.negative_line_id=#{id})")
 long settlements(Long id);
 @Select("SELECT COUNT(*) FROM oms_ar_invoice_allocation a JOIN oms_ar_invoice h ON h.id=a.invoice_id AND h.status='ACTIVE' AND h.delete_status=false WHERE a.delete_status=false AND a.receivable_line_id=#{id}")
 long invoices(Long id);
 @Select("SELECT COUNT(*) FROM oms_ar_balance_lot WHERE source_line_id=#{id} AND status='ACTIVE' AND delete_status=false") long lots(Long id);
 @Select("SELECT l.* FROM oms_ar_adjustment_link a JOIN oms_ar_line l ON l.id=a.line_id AND l.status='ACTIVE' AND l.delete_status=false WHERE a.original_line_id=#{id} AND a.delete_status=false") List<ArLine> dependents(Long id);
}
