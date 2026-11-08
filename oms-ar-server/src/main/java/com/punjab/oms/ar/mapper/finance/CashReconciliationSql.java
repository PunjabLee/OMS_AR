package com.punjab.oms.ar.mapper.finance;

/** 仅比较指定主体、客户与币种的账户、原批次、冻结分摊及提现凭证。 @author Punjab */
public final class CashReconciliationSql {
    private CashReconciliationSql() {}

    private static final String REPORT = """
        WITH accounts AS (
          SELECT * FROM oms_ar_cash_account WHERE legal_entity_id=#{legalEntityId}
            AND customer_id=#{customerId} AND currency=#{currency} AND delete_status=false
        ), lots AS (
          SELECT l.*,r.amount,r.allocated_amount FROM accounts a JOIN oms_ar_cash_receipt_lot l
            ON l.cash_account_id=a.id JOIN oms_ar_receipt r ON r.id=l.receipt_id
            WHERE l.delete_status=false AND r.delete_status=false
        ), holds AS (
          SELECT h.* FROM accounts a JOIN oms_ar_cash_hold h ON h.cash_account_id=a.id AND h.delete_status=false
        ), slices AS (
          SELECT s.* FROM holds h JOIN oms_ar_cash_hold_slice s ON s.hold_id=h.id AND s.delete_status=false
        ), withdrawals AS (
          SELECT w.* FROM accounts a JOIN oms_ar_withdrawal w ON w.cash_account_id=a.id AND w.delete_status=false
        ), checks AS (
          SELECT 'CASH_BOOK' AS rule,'CASH_ACCOUNT' AS object_type,a.id AS object_id,a.book_amount AS stored_amount,
            COALESCE(SUM(l.amount-l.allocated_amount-l.withdrawn_amount),0) AS expected_amount
            FROM accounts a LEFT JOIN lots l ON l.cash_account_id=a.id GROUP BY a.id,a.book_amount
          UNION ALL
          SELECT 'CASH_FROZEN','CASH_ACCOUNT',a.id,a.frozen_amount,COALESCE(SUM(l.frozen_amount),0)
            FROM accounts a LEFT JOIN lots l ON l.cash_account_id=a.id GROUP BY a.id,a.frozen_amount
          UNION ALL
          SELECT 'CASH_LOT_HOLD','CASH_RECEIPT_LOT',l.id,l.frozen_amount,COALESCE(SUM(s.amount-s.released_amount-s.captured_amount),0)
            FROM lots l LEFT JOIN slices s ON s.lot_id=l.id GROUP BY l.id,l.frozen_amount
          UNION ALL
          SELECT 'CASH_LOT_PAID','CASH_RECEIPT_LOT',l.id,l.withdrawn_amount,COALESCE(SUM(s.captured_amount),0)
            FROM lots l LEFT JOIN slices s ON s.lot_id=l.id GROUP BY l.id,l.withdrawn_amount
          UNION ALL
          SELECT 'CASH_RECEIPT_USED','RECEIPT',l.receipt_id,l.allocated_amount,
            COALESCE((SELECT SUM(d.amount) FROM oms_ar_settlement t JOIN oms_ar_settlement_allocation d ON d.settlement_id=t.id
             WHERE t.receipt_id=l.receipt_id AND t.status='POSTED' AND t.delete_status=false AND d.delete_status=false),0)
            FROM lots l
          UNION ALL
          SELECT 'CASH_HOLD_TOTAL','CASH_HOLD',h.id,h.amount,COALESCE(SUM(s.amount),0)
            FROM holds h LEFT JOIN slices s ON s.hold_id=h.id GROUP BY h.id,h.amount
          UNION ALL
          SELECT 'CASH_HOLD_RELEASED','CASH_HOLD',h.id,h.released_amount,COALESCE(SUM(s.released_amount),0)
            FROM holds h LEFT JOIN slices s ON s.hold_id=h.id GROUP BY h.id,h.released_amount
          UNION ALL
          SELECT 'CASH_HOLD_CAPTURED','CASH_HOLD',h.id,h.captured_amount,COALESCE(SUM(s.captured_amount),0)
            FROM holds h LEFT JOIN slices s ON s.hold_id=h.id GROUP BY h.id,h.captured_amount
          UNION ALL
          SELECT 'CASH_WITHDRAWAL_HOLD','WITHDRAWAL',w.id,1,0 FROM withdrawals w JOIN holds h ON h.id=w.hold_id
            WHERE h.amount<>w.amount OR h.purpose<>'WITHDRAWAL' OR
            h.captured_amount<>IF(w.status='PAID',w.amount,0) OR
            h.released_amount<>IF(w.status IN ('CANCELLED','REJECTED','FAILED'),w.amount,0)
          UNION ALL
          SELECT 'CASH_WITHDRAWAL_PROOF','WITHDRAWAL',w.id,1,0 FROM withdrawals w WHERE w.status='PAID'
            AND (w.bank_receipt IS NULL OR TRIM(w.bank_receipt)='' OR NOT EXISTS (
              SELECT 1 FROM oms_ar_lifecycle_event e JOIN oms_ar_lifecycle_change c ON c.event_id=e.id
              WHERE e.event_type='WITHDRAW_PAID' AND c.object_type='WITHDRAWAL' AND c.object_id=w.id
                AND e.delete_status=false AND c.delete_status=false
                AND JSON_UNQUOTE(JSON_EXTRACT(c.after_snapshot,'$.bankReceipt'))=w.bank_receipt))
        )
        """;
    public static String count() {
        return REPORT+"SELECT COUNT(*) FROM checks WHERE stored_amount<>expected_amount";
    }
    public static String page() {
        return REPORT+"SELECT * FROM checks WHERE stored_amount<>expected_amount ORDER BY rule,object_id LIMIT #{size} OFFSET #{offset}";
    }
}
