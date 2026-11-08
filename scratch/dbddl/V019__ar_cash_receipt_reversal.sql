CREATE TABLE IF NOT EXISTS oms_ar_cash_receipt_reversal (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 lot_id BIGINT NOT NULL,
 operation_id BIGINT NOT NULL,
 amount DECIMAL(24,8) NOT NULL,
 reason VARCHAR(500) NOT NULL,
 created_time DATETIME(6) NOT NULL,created_by VARCHAR(100) NOT NULL,
 updated_time DATETIME(6) NOT NULL,updated_by VARCHAR(100) NOT NULL,delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_cash_reversed_lot(lot_id),
 UNIQUE KEY uk_cash_reversal_operation(operation_id),
 CONSTRAINT fk_cash_reversal_lot FOREIGN KEY(lot_id) REFERENCES oms_ar_cash_receipt_lot(id),
 CONSTRAINT fk_cash_reversal_operation FOREIGN KEY(operation_id) REFERENCES oms_ar_operation_request(id),
 CONSTRAINT ck_cash_reversal_amount CHECK(amount>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
