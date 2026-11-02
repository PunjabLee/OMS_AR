CREATE TABLE oms_ar_customer_account (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 customer_id BIGINT NOT NULL,legal_entity_id BIGINT NOT NULL,currency CHAR(3) NOT NULL,
 balance_amount DECIMAL(24,8) NOT NULL DEFAULT 0,version BIGINT NOT NULL DEFAULT 0,
 created_time DATETIME(6) NOT NULL,created_by VARCHAR(100) NOT NULL,
 updated_time DATETIME(6) NOT NULL,updated_by VARCHAR(100) NOT NULL,delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_account_scope(customer_id,legal_entity_id,currency),
 UNIQUE KEY uk_account_id_scope(id,customer_id,legal_entity_id,currency),
 CONSTRAINT ck_account_balance CHECK(balance_amount>=0),CONSTRAINT ck_account_version CHECK(version>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
CREATE TABLE oms_ar_balance_lot (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 account_id BIGINT NOT NULL,source_line_id BIGINT NOT NULL,
 customer_id BIGINT NOT NULL,legal_entity_id BIGINT NOT NULL,currency CHAR(3) NOT NULL,
 amount DECIMAL(24,8) NOT NULL,used_amount DECIMAL(24,8) NOT NULL DEFAULT 0,
 status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',version BIGINT NOT NULL DEFAULT 0,
 created_time DATETIME(6) NOT NULL,created_by VARCHAR(100) NOT NULL,
 updated_time DATETIME(6) NOT NULL,updated_by VARCHAR(100) NOT NULL,delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 KEY ix_lot_account(account_id,status,id),KEY ix_lot_source(source_line_id),
 CONSTRAINT fk_lot_account FOREIGN KEY(account_id,customer_id,legal_entity_id,currency) REFERENCES oms_ar_customer_account(id,customer_id,legal_entity_id,currency),
 CONSTRAINT fk_lot_source FOREIGN KEY(source_line_id) REFERENCES oms_ar_line(id),
 CONSTRAINT ck_lot_amount CHECK(amount>0 AND used_amount>=0 AND used_amount<=amount),
 CONSTRAINT ck_lot_status CHECK(status IN ('ACTIVE','REVERSED') AND (status='ACTIVE' OR used_amount=0)),
 CONSTRAINT ck_lot_version CHECK(version>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
