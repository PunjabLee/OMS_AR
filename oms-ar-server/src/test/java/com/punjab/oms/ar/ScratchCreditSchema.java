package com.punjab.oms.ar;

import java.nio.file.Path;
import org.springframework.jdbc.core.JdbcTemplate;

/** 在重复使用的联测库中仅执行一次信用账户结构升级。 @author Punjab */
public final class ScratchCreditSchema {
    private ScratchCreditSchema() {}

    public static boolean shouldApply(JdbcTemplate jdbc, Path file) {
        if (!file.getFileName().toString().equals("V018__ar_credit_cash_scope.sql")) return true;
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
              AND table_name='oms_ar_customer_account' AND column_name='cash_account_id'
            """, Integer.class) == 0;
    }
}
