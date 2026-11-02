package com.punjab.oms.ar.mapper.finance;
/** 固定对账SQL，所有输入使用绑定参数；总数与分页共享规则。 @author Punjab */
public final class ReconciliationSql {
 private ReconciliationSql(){}
 private static final String DIFFERENCES="""
 SELECT * FROM (
SELECT 'LINE_SETTLED' AS rule,'LINE' AS object_type,r.id AS object_id,r.settled_amount AS stored_amount,(SELECT COALESCE(SUM(a.amount),0) FROM oms_ar_settlement_allocation a JOIN oms_ar_settlement h ON h.id=a.settlement_id AND h.status='POSTED' AND h.delete_status=false WHERE a.positive_line_id=r.id AND a.delete_status=false)-(SELECT COALESCE(SUM(a.amount),0) FROM oms_ar_settlement_allocation a JOIN oms_ar_settlement h ON h.id=a.settlement_id AND h.status='POSTED' AND h.delete_status=false WHERE a.negative_line_id=r.id AND a.delete_status=false) AS expected_amount FROM oms_ar_line r WHERE r.delete_status=false AND r.legal_entity_id=#{legalEntityId} AND r.customer_id=#{customerId} AND r.currency=#{currency}
 UNION ALL
SELECT 'LINE_TRANSFERRED' AS rule,'LINE' AS object_type,r.id AS object_id,r.transferred_amount AS stored_amount,-(SELECT COALESCE(SUM(a.amount),0) FROM oms_ar_balance_lot a WHERE a.source_line_id=r.id AND a.status='ACTIVE' AND a.delete_status=false) AS expected_amount FROM oms_ar_line r WHERE r.delete_status=false AND r.legal_entity_id=#{legalEntityId} AND r.customer_id=#{customerId} AND r.currency=#{currency}
 UNION ALL
SELECT 'LINE_INVOICED' AS rule,'LINE' AS object_type,r.id AS object_id,r.invoiced_amount AS stored_amount,(SELECT COALESCE(SUM(a.amount),0) FROM oms_ar_invoice_allocation a JOIN oms_ar_invoice h ON h.id=a.invoice_id AND h.status='ACTIVE' AND h.delete_status=false WHERE a.receivable_line_id=r.id AND a.delete_status=false) AS expected_amount FROM oms_ar_line r WHERE r.delete_status=false AND r.legal_entity_id=#{legalEntityId} AND r.customer_id=#{customerId} AND r.currency=#{currency}
 UNION ALL
SELECT 'RECEIPT_ALLOCATED' AS rule,'RECEIPT' AS object_type,r.id AS object_id,r.allocated_amount AS stored_amount,(SELECT COALESCE(SUM(a.amount),0) FROM oms_ar_settlement a WHERE a.receipt_id=r.id AND a.status='POSTED' AND a.delete_status=false) AS expected_amount FROM oms_ar_receipt r WHERE r.delete_status=false AND r.legal_entity_id=#{legalEntityId} AND r.customer_id=#{customerId} AND r.currency=#{currency}
 UNION ALL
SELECT 'ACCOUNT_BALANCE' AS rule,'ACCOUNT' AS object_type,r.id AS object_id,r.balance_amount AS stored_amount,(SELECT COALESCE(SUM(a.amount-a.used_amount),0) FROM oms_ar_balance_lot a WHERE a.account_id=r.id AND a.status='ACTIVE' AND a.delete_status=false) AS expected_amount FROM oms_ar_customer_account r WHERE r.delete_status=false AND r.legal_entity_id=#{legalEntityId} AND r.customer_id=#{customerId} AND r.currency=#{currency}
 UNION ALL
SELECT 'LOT_USED' AS rule,'LOT' AS object_type,r.id AS object_id,r.used_amount AS stored_amount,(SELECT COALESCE(SUM(a.amount),0) FROM oms_ar_balance_usage a JOIN oms_ar_settlement h ON h.id=a.settlement_id AND h.status='POSTED' AND h.delete_status=false WHERE a.lot_id=r.id AND a.delete_status=false) AS expected_amount FROM oms_ar_balance_lot r WHERE r.delete_status=false AND r.legal_entity_id=#{legalEntityId} AND r.customer_id=#{customerId} AND r.currency=#{currency}
 UNION ALL
SELECT 'SETTLEMENT_TOTAL' AS rule,'SETTLEMENT' AS object_type,r.id AS object_id,r.amount AS stored_amount,(SELECT COALESCE(SUM(a.amount),0) FROM oms_ar_settlement_allocation a WHERE a.settlement_id=r.id AND a.delete_status=false) AS expected_amount FROM oms_ar_settlement r WHERE r.delete_status=false AND r.legal_entity_id=#{legalEntityId} AND r.customer_id=#{customerId} AND r.currency=#{currency}
 UNION ALL
SELECT 'BALANCE_ALLOCATION_USAGE' AS rule,'ALLOCATION' AS object_type,r.id AS object_id,r.amount AS stored_amount,(SELECT COALESCE(SUM(u.amount),0) FROM oms_ar_balance_usage u WHERE u.allocation_id=r.id AND u.settlement_id=r.settlement_id AND u.delete_status=false) AS expected_amount FROM oms_ar_settlement_allocation r JOIN oms_ar_settlement h ON h.id=r.settlement_id WHERE r.delete_status=false AND h.delete_status=false AND h.settlement_type='BALANCE' AND h.legal_entity_id=#{legalEntityId} AND h.customer_id=#{customerId} AND h.currency=#{currency}
 UNION ALL
SELECT 'INVOICE_TOTAL' AS rule,'INVOICE' AS object_type,r.id AS object_id,r.total_amount AS stored_amount,(SELECT COALESCE(SUM(a.amount),0) FROM oms_ar_invoice_allocation a WHERE a.invoice_id=r.id AND a.delete_status=false) AS expected_amount FROM oms_ar_invoice r WHERE r.delete_status=false AND r.legal_entity_id=#{legalEntityId} AND r.customer_id=#{customerId} AND r.currency=#{currency}
 UNION ALL
SELECT 'INVOICE_RED_EXCESS' AS rule,'INVOICE_ALLOCATION' AS object_type,r.id AS object_id,(SELECT COALESCE(-SUM(a.amount),0) FROM oms_ar_invoice_allocation a JOIN oms_ar_invoice red ON red.id=a.invoice_id AND red.status='ACTIVE' AND red.delete_status=false WHERE a.original_allocation_id=r.id AND a.delete_status=false) AS stored_amount,r.amount AS expected_amount FROM oms_ar_invoice_allocation r JOIN oms_ar_invoice blue ON blue.id=r.invoice_id WHERE r.delete_status=false AND blue.delete_status=false AND blue.invoice_kind='BLUE' AND blue.legal_entity_id=#{legalEntityId} AND blue.customer_id=#{customerId} AND blue.currency=#{currency}
 ) checks WHERE (rule='INVOICE_RED_EXCESS' AND stored_amount > expected_amount) OR (rule<>'INVOICE_RED_EXCESS' AND stored_amount <> expected_amount)
 """;
 public static String count(){return "SELECT COUNT(*) FROM ("+DIFFERENCES+") differences";}
 public static String page(){return DIFFERENCES+" ORDER BY rule,object_id LIMIT #{size} OFFSET #{offset}";}
}
