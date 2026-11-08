CREATE TABLE oms_ar_cash_hold (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 cash_account_id BIGINT NOT NULL,
 purpose VARCHAR(16) NOT NULL,
 amount DECIMAL(24,8) NOT NULL,
 released_amount DECIMAL(24,8) NOT NULL DEFAULT 0,
 captured_amount DECIMAL(24,8) NOT NULL DEFAULT 0,
 version BIGINT NOT NULL DEFAULT 0,
 created_time DATETIME(6) NOT NULL,created_by VARCHAR(100) NOT NULL,
 updated_time DATETIME(6) NOT NULL,updated_by VARCHAR(100) NOT NULL,delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_hold_account(id,cash_account_id),
 FOREIGN KEY(cash_account_id) REFERENCES oms_ar_cash_account(id),
 CHECK(purpose IN ('MANUAL','WITHDRAWAL') AND (purpose='WITHDRAWAL' OR captured_amount=0)),
 CHECK(amount>0 AND released_amount>=0 AND captured_amount>=0 AND released_amount+captured_amount<=amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
CREATE TABLE oms_ar_cash_hold_slice (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 hold_id BIGINT NOT NULL,cash_account_id BIGINT NOT NULL,lot_id BIGINT NOT NULL,
 amount DECIMAL(24,8) NOT NULL,released_amount DECIMAL(24,8) NOT NULL DEFAULT 0,captured_amount DECIMAL(24,8) NOT NULL DEFAULT 0,
 created_time DATETIME(6) NOT NULL,created_by VARCHAR(100) NOT NULL,
 updated_time DATETIME(6) NOT NULL,updated_by VARCHAR(100) NOT NULL,delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_hold_lot(hold_id,lot_id),
 FOREIGN KEY(hold_id,cash_account_id) REFERENCES oms_ar_cash_hold(id,cash_account_id),
 FOREIGN KEY(lot_id) REFERENCES oms_ar_cash_receipt_lot(id),
 CHECK(amount>0 AND released_amount>=0 AND captured_amount>=0 AND released_amount+captured_amount<=amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
