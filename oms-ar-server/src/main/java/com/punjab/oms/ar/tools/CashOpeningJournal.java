package com.punjab.oms.ar.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.punjab.oms.ar.common.utils.FinanceValues;
import com.punjab.oms.ar.dao.finance.OutboxDao;
import com.punjab.oms.ar.domain.entity.finance.*;
import com.punjab.oms.ar.service.finance.impl.OutboxEnqueueServiceImpl;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import java.math.BigDecimal;
import java.util.*;

/** 在切换事务中记录分户前后余额，共用业务服务的通知序列化格式。 */
final class CashOpeningJournal {
    private final JdbcTemplate db;
    private final com.punjab.oms.ar.common.utils.FinanceSnapshotJson snapshots = new com.punjab.oms.ar.common.utils.FinanceSnapshotJson(CashCutover.JSON);
    private final CashCutover.Manifest scope;

    CashOpeningJournal(JdbcTemplate db, CashCutover.Manifest scope) {
        this.db = db;
        this.scope = scope;
    }

    record Account(String type, long id) {}

    Map<Account, Map<String,Object>> accounts() {
        var result = new LinkedHashMap<Account, Map<String,Object>>();
        collect(result, "CASH_ACCOUNT", "cash_account", "book_amount AS bookAmount,frozen_amount AS frozenAmount");
        collect(result, "CUSTOMER_ACCOUNT", "customer_account", "balance_amount AS balanceAmount");
        return result;
    }

    private void collect(Map<Account, Map<String,Object>> result, String type, String table, String amounts) {
        String query = "SELECT id,version," + amounts + " FROM oms_ar_" + table + " WHERE " + CashCutover.SCOPE + " ORDER BY id";
        for (var row : db.queryForList(query, scope.legalEntityId(), scope.customerId(), scope.currency())) {
            result.put(new Account(type, CashCutover.id(row,"id")),row);
        }
    }

    long record(String fingerprint, Map<Account, Map<String,Object>> before) {
        String actor = scope.actor();
        db.update("""
            INSERT INTO oms_ar_lifecycle_event(event_type,occurred_at,actor,reason,trigger_type,trigger_id,correlation_id,schema_version,created_by,updated_by)
            VALUES('CASH_CUTOVER',NOW(6),?,?,'CASH_CUTOVER',?,?,2,?,?)
            """, actor, scope.reason(), fingerprint, fingerprint, actor, actor);
        long event = db.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        accounts().forEach((key, after) -> {
            var prior = before.get(key);
            var delta = new TreeMap<String, BigDecimal>();
            after.forEach((name,value) -> {
                if (value instanceof BigDecimal amount) {
                    delta.put(name, amount.subtract(prior == null ? BigDecimal.ZERO : (BigDecimal)prior.get(name)));
                }
            });
            db.update("""
                INSERT INTO oms_ar_lifecycle_change(event_id,object_type,object_id,before_snapshot,delta_snapshot,after_snapshot,
                before_version,after_version,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?,?)
                """, event,key.type(),key.id(),prior == null ? null : snapshots.normalize(CashCutover.json(prior)),snapshots.normalize(CashCutover.json(delta)),
                    snapshots.normalize(CashCutover.json(after)),prior == null ? null : prior.get("version"),after.get("version"),actor,actor);
        });
        var mapper = new ObjectMapper().findAndRegisterModules().disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        new OutboxEnqueueServiceImpl(new JournalRows(),new FinanceValues(mapper),mapper).enqueue(event,scope.legalEntityId(),actor);
        return event;
    }

    /** 复用当前 JDBC 事务，命令行无需启动应用及后台派发任务。 */
    private final class JournalRows implements OutboxDao {
        public ArEvent event(Long id) {
            return db.queryForObject("SELECT * FROM oms_ar_lifecycle_event WHERE id=?",new BeanPropertyRowMapper<>(ArEvent.class),id);
        }
        public List<ArChange> changes(Long event) {
            return db.query("SELECT * FROM oms_ar_lifecycle_change WHERE event_id=? ORDER BY id",new BeanPropertyRowMapper<>(ArChange.class),event);
        }
        public boolean create(ArOutbox row) {
            return db.update("""
                INSERT INTO oms_ar_outbox(event_id,legal_entity_id,destination,payload,created_by,updated_by)
                VALUES(?,?,?,?,?,?)
                """,row.getEventId(),row.getLegalEntityId(),row.getDestination(),row.getPayload(),row.getCreatedBy(),row.getUpdatedBy()) == 1;
        }
    }
}
