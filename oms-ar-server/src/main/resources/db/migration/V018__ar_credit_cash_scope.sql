ALTER TABLE oms_ar_customer_account
 ADD COLUMN cash_account_id BIGINT NULL,
 ADD COLUMN cash_scope_key BIGINT GENERATED ALWAYS AS (IFNULL(cash_account_id,0)) STORED,
 DROP INDEX uk_account_scope,
 ADD UNIQUE KEY uk_credit_cash_scope(customer_id,legal_entity_id,currency,cash_scope_key),
 ADD CONSTRAINT fk_credit_cash_scope FOREIGN KEY(cash_account_id,customer_id,legal_entity_id,currency)
 REFERENCES oms_ar_cash_account(id,customer_id,legal_entity_id,currency);
