CREATE TABLE IF NOT EXISTS oms_ar_payout_account (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 legal_entity_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, currency CHAR(3) NOT NULL,
 beneficiary_ref VARCHAR(100) NOT NULL, account_last_four CHAR(4) NOT NULL,
 status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE', version BIGINT NOT NULL DEFAULT 0,
 created_by VARCHAR(100) NOT NULL, updated_by VARCHAR(100) NOT NULL,
 created_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_payout_ref(legal_entity_id,beneficiary_ref),
 UNIQUE KEY uk_payout_scope(id,customer_id,legal_entity_id,currency),
 CONSTRAINT ck_payout_status CHECK(status IN ('ACTIVE','DISABLED')),
 CONSTRAINT ck_payout_suffix CHECK(account_last_four REGEXP '^[0-9]{4}$')
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;

CREATE TABLE IF NOT EXISTS oms_ar_withdrawal (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 customer_id BIGINT NOT NULL, legal_entity_id BIGINT NOT NULL, currency CHAR(3) NOT NULL,
 cash_account_id BIGINT NOT NULL, payout_account_id BIGINT NOT NULL, hold_id BIGINT NOT NULL,
 amount DECIMAL(24,8) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
 reviewer VARCHAR(100) NULL, claimed_by VARCHAR(100) NULL, bank_receipt VARCHAR(128) NULL,
 evidence VARCHAR(256) NULL, version BIGINT NOT NULL DEFAULT 0,
 created_by VARCHAR(100) NOT NULL, updated_by VARCHAR(100) NOT NULL,
 created_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), updated_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_withdrawal_hold(hold_id), UNIQUE KEY uk_withdrawal_bank_receipt(legal_entity_id,bank_receipt),
 CONSTRAINT fk_withdrawal_cash FOREIGN KEY(cash_account_id,customer_id,legal_entity_id,currency) REFERENCES oms_ar_cash_account(id,customer_id,legal_entity_id,currency),
 CONSTRAINT fk_withdrawal_payout FOREIGN KEY(payout_account_id,customer_id,legal_entity_id,currency) REFERENCES oms_ar_payout_account(id,customer_id,legal_entity_id,currency),
 CONSTRAINT fk_withdrawal_hold FOREIGN KEY(hold_id,cash_account_id) REFERENCES oms_ar_cash_hold(id,cash_account_id),
 CONSTRAINT ck_withdrawal_amount CHECK(amount>0),
 CONSTRAINT ck_withdrawal_status CHECK(status IN ('PENDING','APPROVED','REJECTED','CANCELLED','PROCESSING','UNKNOWN','PAID','FAILED')),
 CONSTRAINT ck_withdrawal_receipt CHECK((status='PAID' AND bank_receipt IS NOT NULL) OR (status<>'PAID' AND bank_receipt IS NULL)),
 CONSTRAINT ck_withdrawal_evidence CHECK(status<>'FAILED' OR evidence IS NOT NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
