CREATE TABLE oms_ar_receipt (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 customer_id BIGINT NOT NULL,legal_entity_id BIGINT NOT NULL,currency CHAR(3) NOT NULL,
 amount DECIMAL(24,8) NOT NULL,allocated_amount DECIMAL(24,8) NOT NULL DEFAULT 0,
 version BIGINT NOT NULL DEFAULT 0,received_date DATE NOT NULL,
 source_system VARCHAR(64) NOT NULL,source_id VARCHAR(128) NOT NULL,source_no VARCHAR(128) NOT NULL,
 created_time DATETIME(6) NOT NULL,created_by VARCHAR(100) NOT NULL,
 updated_time DATETIME(6) NOT NULL,updated_by VARCHAR(100) NOT NULL,
 delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_receipt_source(legal_entity_id,source_system,source_id),
 UNIQUE KEY uk_receipt_scope(id,customer_id,legal_entity_id,currency),
 KEY ix_receipt_customer(legal_entity_id,customer_id,currency,id),
 CONSTRAINT ck_receipt_amount CHECK(amount>0 AND allocated_amount>=0 AND allocated_amount<=amount),
 CONSTRAINT ck_receipt_version CHECK(version>=0),
 CONSTRAINT ck_receipt_scope CHECK(customer_id>0 AND legal_entity_id>0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
