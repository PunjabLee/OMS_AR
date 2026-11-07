CREATE TABLE oms_ar_cash_group_binding (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 group_id BIGINT NOT NULL,
 cash_account_id BIGINT NOT NULL,
 customer_id BIGINT NOT NULL,
 legal_entity_id BIGINT NOT NULL,
 currency CHAR(3) NOT NULL,
 created_time DATETIME(6) NOT NULL,
 created_by VARCHAR(100) NOT NULL,
 updated_time DATETIME(6) NOT NULL,
 updated_by VARCHAR(100) NOT NULL,
 delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_cash_binding_group(group_id),
 KEY ix_cash_binding_account(cash_account_id,group_id),
 CONSTRAINT fk_cash_binding_group FOREIGN KEY(group_id,customer_id,legal_entity_id,currency)
   REFERENCES oms_ar_business_group(id,customer_id,legal_entity_id,currency),
 CONSTRAINT fk_cash_binding_account FOREIGN KEY(cash_account_id,customer_id,legal_entity_id,currency)
   REFERENCES oms_ar_cash_account(id,customer_id,legal_entity_id,currency)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
