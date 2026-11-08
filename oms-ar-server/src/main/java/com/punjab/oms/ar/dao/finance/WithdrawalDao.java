package com.punjab.oms.ar.dao.finance;

import com.punjab.oms.ar.domain.vo.finance.WithdrawalResponse;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;

/** 提现写入使用同一连接上的行锁以及范围外键。 @author Punjab */
@Repository @RequiredArgsConstructor
public class WithdrawalDao {
    private final JdbcClient db;
    public record Account(long id,long customerId,String currency,String status) {}
    public record Payout(long id,long customerId,String currency,String status,String lastFour) {}
    public Account account(long id,long scope) {
        return db.sql("SELECT id,customer_id,currency,status FROM oms_ar_cash_account WHERE id=? AND legal_entity_id=? AND delete_status=false FOR UPDATE")
                .params(id,scope).query(Account.class).optional().orElse(null);
    }
    public Payout payout(long id,long scope) {
        return db.sql("SELECT id,customer_id,currency,status,account_last_four AS last_four FROM oms_ar_payout_account WHERE id=? AND legal_entity_id=? AND delete_status=false FOR UPDATE")
                .params(id,scope).query(Payout.class).optional().orElse(null);
    }
    public long bind(long scope,long customer,String currency,String ref,String suffix,String actor) {
        return insert(db.sql("INSERT INTO oms_ar_payout_account(legal_entity_id,customer_id,currency,beneficiary_ref,account_last_four,created_by,updated_by) VALUES(?,?,?,?,?,?,?)")
                .params(scope,customer,currency,ref,suffix,actor,actor));
    }
    public void disable(long id,String actor) {
        db.sql("UPDATE oms_ar_payout_account SET status='DISABLED',version=version+1,updated_by=?,updated_time=NOW(6) WHERE id=?").params(actor,id).update();
    }
    public long apply(long scope,Account account,long payoutId,long holdId,BigDecimal amount,String actor) {
        return insert(db.sql("INSERT INTO oms_ar_withdrawal(customer_id,legal_entity_id,currency,cash_account_id,payout_account_id,hold_id,amount,created_by,updated_by) VALUES(?,?,?,?,?,?,?,?,?)")
                .params(account.customerId(),scope,account.currency(),account.id(),payoutId,holdId,amount,actor,actor));
    }
    private long insert(JdbcClient.StatementSpec stmt) {
        var key=new GeneratedKeyHolder();
        stmt.update(key,"id");
        return java.util.Objects.requireNonNull(key.getKey()).longValue();
    }
    public record Row(long id,long cashAccountId,long payoutAccountId,long holdId,String status,String createdBy,long version) {}
    public Row lock(long scope,long id) {
        return db.sql("SELECT id,cash_account_id,payout_account_id,hold_id,status,created_by,version FROM oms_ar_withdrawal WHERE id=? AND legal_entity_id=? AND delete_status=false FOR UPDATE")
                .params(id,scope).query(Row.class).optional().orElse(null);
    }
    public void change(long id,String status,String actor,String receipt,String evidence) {
        db.sql("""
            UPDATE oms_ar_withdrawal SET status=?, version=version+1, updated_by=?, updated_time=NOW(6),
            reviewer=CASE WHEN ? IN ('APPROVED','REJECTED') THEN ? ELSE reviewer END,
            claimed_by=CASE WHEN ?='PROCESSING' THEN ? ELSE claimed_by END, bank_receipt=?, evidence=?
            WHERE id=?
            """).params(java.util.Arrays.asList(status,actor,status,actor,status,actor,receipt,evidence,id)).update();
    }
    public WithdrawalResponse detail(long scope,long id) {
        return db.sql("""
            SELECT w.id,w.cash_account_id,w.payout_account_id,CONCAT('****',p.account_last_four) masked_account,
            w.currency,w.amount,w.status,w.created_by applicant,w.reviewer,w.claimed_by,w.bank_receipt,w.evidence,w.version
            FROM oms_ar_withdrawal w JOIN oms_ar_payout_account p ON p.id=w.payout_account_id
            WHERE w.legal_entity_id=? AND w.id=? AND w.delete_status=false
            """).params(scope,id).query(WithdrawalResponse.class).optional().orElse(null);
    }
}
