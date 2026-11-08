CREATE TABLE oms_ar_cash_cutover (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 legal_entity_id BIGINT NOT NULL, customer_id BIGINT NOT NULL, currency CHAR(3) NOT NULL,
 manifest_hash CHAR(64) NOT NULL, fingerprint CHAR(64) NOT NULL,
 manifest JSON NOT NULL, opening_report JSON NOT NULL,
 created_by VARCHAR(100) NOT NULL, created_time DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uk_cutover_scope(legal_entity_id,customer_id,currency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
