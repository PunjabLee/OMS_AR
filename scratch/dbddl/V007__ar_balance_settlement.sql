CREATE TABLE IF NOT EXISTS oms_ar_balance_settlement (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,settlement_id BIGINT NOT NULL,account_id BIGINT NOT NULL,
 created_time DATETIME(6) NOT NULL,created_by VARCHAR(100) NOT NULL,
 updated_time DATETIME(6) NOT NULL,updated_by VARCHAR(100) NOT NULL,delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_balance_settlement(settlement_id),KEY ix_balance_settlement_account(account_id,settlement_id),
 CONSTRAINT fk_bs_settlement FOREIGN KEY(settlement_id) REFERENCES oms_ar_settlement(id),
 CONSTRAINT fk_bs_account FOREIGN KEY(account_id) REFERENCES oms_ar_customer_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
CREATE TABLE IF NOT EXISTS oms_ar_balance_usage (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,settlement_id BIGINT NOT NULL,allocation_id BIGINT NOT NULL,lot_id BIGINT NOT NULL,
 sequence_no INT NOT NULL,amount DECIMAL(24,8) NOT NULL,
 created_time DATETIME(6) NOT NULL,created_by VARCHAR(100) NOT NULL,
 updated_time DATETIME(6) NOT NULL,updated_by VARCHAR(100) NOT NULL,delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_balance_usage_sequence(settlement_id,sequence_no),UNIQUE KEY uk_balance_usage_allocation_lot(allocation_id,lot_id),KEY ix_usage_lot(lot_id,settlement_id),
 CONSTRAINT fk_usage_settlement FOREIGN KEY(settlement_id) REFERENCES oms_ar_settlement(id),
 CONSTRAINT fk_usage_allocation FOREIGN KEY(allocation_id) REFERENCES oms_ar_settlement_allocation(id),
 CONSTRAINT fk_usage_lot FOREIGN KEY(lot_id) REFERENCES oms_ar_balance_lot(id),
 CONSTRAINT ck_usage_amount CHECK(amount>0 AND sequence_no>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
