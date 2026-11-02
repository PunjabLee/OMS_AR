CREATE TABLE oms_ar_adjustment_link (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 line_id BIGINT NOT NULL,related_group_id BIGINT NOT NULL,original_line_id BIGINT NULL,
 created_time DATETIME(6) NOT NULL,created_by VARCHAR(100) NOT NULL,
 updated_time DATETIME(6) NOT NULL,updated_by VARCHAR(100) NOT NULL,delete_status BOOLEAN NOT NULL DEFAULT FALSE,
 UNIQUE KEY uk_adjustment_line(line_id),KEY ix_adjustment_original(original_line_id),
 CONSTRAINT fk_adjustment_line FOREIGN KEY(line_id) REFERENCES oms_ar_line(id),
 CONSTRAINT fk_adjustment_group FOREIGN KEY(related_group_id) REFERENCES oms_ar_business_group(id),
 CONSTRAINT fk_adjustment_original FOREIGN KEY(original_line_id) REFERENCES oms_ar_line(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_as_cs;
