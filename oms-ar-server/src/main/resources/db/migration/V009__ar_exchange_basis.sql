CREATE TABLE oms_ar_exchange_basis (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,line_id BIGINT NOT NULL,
 original_currency CHAR(3) NOT NULL,original_amount DECIMAL(24,8) NOT NULL,
 old_rate DECIMAL(24,12) NOT NULL,new_rate DECIMAL(24,12) NOT NULL,rate_direction VARCHAR(32) NOT NULL,
 old_settlement_amount DECIMAL(24,8) NOT NULL,new_settlement_amount DECIMAL(24,8) NOT NULL,confirmed_difference DECIMAL(24,8) NOT NULL,
 created_time DATETIME(6) NOT NULL,created_by VARCHAR(100) NOT NULL,
 updated_time DATETIME(6) NOT NULL,updated_by VARCHAR(100) NOT NULL,delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_exchange_line(line_id),CONSTRAINT fk_exchange_line FOREIGN KEY(line_id) REFERENCES oms_ar_line(id),
 CONSTRAINT ck_exchange_rates CHECK(old_rate>0 AND new_rate>0),CONSTRAINT ck_exchange_original CHECK(original_amount<>0),
 CONSTRAINT ck_exchange_direction CHECK(rate_direction IN ('SETTLEMENT_PER_ORIGINAL','ORIGINAL_PER_SETTLEMENT')),
 CONSTRAINT ck_exchange_difference CHECK(confirmed_difference<>0 AND confirmed_difference=new_settlement_amount-old_settlement_amount)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
