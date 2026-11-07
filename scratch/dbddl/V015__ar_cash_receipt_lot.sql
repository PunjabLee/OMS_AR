CREATE TABLE IF NOT EXISTS oms_ar_cash_receipt_lot (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 receipt_id BIGINT NOT NULL,
 cash_account_id BIGINT NOT NULL,
 customer_id BIGINT NOT NULL,
 legal_entity_id BIGINT NOT NULL,
 currency CHAR(3) NOT NULL,
 frozen_amount DECIMAL(24,8) NOT NULL DEFAULT 0,
 withdrawn_amount DECIMAL(24,8) NOT NULL DEFAULT 0,
 created_time DATETIME(6) NOT NULL,
 created_by VARCHAR(100) NOT NULL,
 updated_time DATETIME(6) NOT NULL,
 updated_by VARCHAR(100) NOT NULL,
 delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_cash_lot_receipt(receipt_id),
 KEY ix_cash_lot_fifo(cash_account_id,id),
 CONSTRAINT fk_cash_lot_receipt FOREIGN KEY(receipt_id,customer_id,legal_entity_id,currency)
   REFERENCES oms_ar_receipt(id,customer_id,legal_entity_id,currency),
 CONSTRAINT fk_cash_lot_account FOREIGN KEY(cash_account_id,customer_id,legal_entity_id,currency)
   REFERENCES oms_ar_cash_account(id,customer_id,legal_entity_id,currency),
 CONSTRAINT ck_cash_lot_amounts CHECK(frozen_amount>=0 AND withdrawn_amount>=0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
