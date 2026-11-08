package com.punjab.oms.ar.mapper.finance;

import com.punjab.oms.ar.domain.bo.finance.*;
import org.apache.ibatis.annotations.*;
import java.util.List;

/** 授权范围内按批次和审计记录读取，金额使用原数据库精度。 @author Punjab */
@Mapper
public interface CashHistoryMapper {
    String LOTS = """
         FROM oms_ar_cash_receipt_lot l JOIN oms_ar_receipt r ON r.id=l.receipt_id
         WHERE l.cash_account_id=#{accountId} AND l.legal_entity_id=#{legalEntityId}
           AND l.delete_status=false AND r.delete_status=false
        """;
    String ENTRIES = """
         FROM oms_ar_cash_account a JOIN oms_ar_lifecycle_change c ON c.object_id=a.id AND c.object_type='CASH_ACCOUNT'
         JOIN oms_ar_lifecycle_event e ON e.id=c.event_id
         WHERE a.id=#{accountId} AND a.legal_entity_id=#{legalEntityId}
           AND a.delete_status=false AND c.delete_status=false AND e.delete_status=false
        """;
    String PAGE=" LIMIT #{size} OFFSET #{offset}";
    @Select("SELECT COUNT(*)"+LOTS) long countLots(CashHistoryScope query);
    @Select("SELECT COUNT(*)"+ENTRIES) long countEntries(CashHistoryScope query);
    @Select("""
        SELECT l.id AS lotId,r.id AS receiptId,r.source_no,r.amount,r.allocated_amount AS allocated,
               l.frozen_amount AS frozen,l.withdrawn_amount AS withdrawn,
               r.amount-r.allocated_amount-l.frozen_amount-l.withdrawn_amount AS available
        """+LOTS+" ORDER BY l.id DESC"+PAGE)
    List<CashLotRow> lots(CashHistoryScope query);
    @Select("""
        SELECT c.id AS changeId,e.id AS eventId,e.event_type,e.occurred_at,e.actor,e.reason,
               JSON_VALUE(c.before_snapshot,'$.bookAmount' RETURNING DECIMAL(24,8)) AS bookBefore,
               JSON_VALUE(c.after_snapshot,'$.bookAmount' RETURNING DECIMAL(24,8)) AS bookAfter,
               JSON_VALUE(c.before_snapshot,'$.frozenAmount' RETURNING DECIMAL(24,8)) AS frozenBefore,
               JSON_VALUE(c.after_snapshot,'$.frozenAmount' RETURNING DECIMAL(24,8)) AS frozenAfter
        """+ENTRIES+" ORDER BY c.id DESC"+PAGE)
    List<CashEntryRow> entries(CashHistoryScope query);
}
