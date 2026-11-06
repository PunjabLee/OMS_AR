# OMS 应收系统数据字典

适用版本为 MySQL 8.0.42，范围为正式迁移 V001–V012 的 21 张业务表，共 311 个列定义。下方可按中文名称跳转到对应物理表。

## 读表约定

- “显式默认值”栏的破折号表示建表语句未声明 DEFAULT。自增主键和生成列的取值方式见附加定义。
- 金额保留业务方向。有效应收行的未核销余额等于原额减已核销额、再减转出额。价款与销项税按 amount_type 分行保存，已开票金额独立累计。
- 客户、核算主体和结算币种共同界定结算范围。来源事件输出按来源系统、事件 ID 和输出键去重。
- PRIMARY KEY 为主键，UNIQUE KEY 为唯一索引，KEY 为普通索引。复合外键按列书写顺序逐项匹配，CHECK 列出数据库强制执行的条件。
- delete_status 表示逻辑删除。作废、反核销及发票关联撤销使用各自的业务状态，历史数据继续保留。

## 表索引

| 业务实体 | 物理表 | 引入版本 |
| --- | --- | --- |
| [应收单头](#oms_ar_document) | `oms_ar_document` | V001 |
| [业务组](#oms_ar_business_group) | `oms_ar_business_group` | V001 |
| [应收明细](#oms_ar_line) | `oms_ar_line` | V001 |
| [应收明细来源](#oms_ar_line_source) | `oms_ar_line_source` | V001 |
| [幂等操作请求](#oms_ar_operation_request) | `oms_ar_operation_request` | V002 |
| [生命周期事件](#oms_ar_lifecycle_event) | `oms_ar_lifecycle_event` | V002 |
| [对象变化快照](#oms_ar_lifecycle_change) | `oms_ar_lifecycle_change` | V002 |
| [实际收款](#oms_ar_receipt) | `oms_ar_receipt` | V003 |
| [核销单](#oms_ar_settlement) | `oms_ar_settlement` | V004 |
| [核销分配](#oms_ar_settlement_allocation) | `oms_ar_settlement_allocation` | V004 |
| [负数调整关联](#oms_ar_adjustment_link) | `oms_ar_adjustment_link` | V005 |
| [客户信用账户](#oms_ar_customer_account) | `oms_ar_customer_account` | V006 |
| [信用余额来源批次](#oms_ar_balance_lot) | `oms_ar_balance_lot` | V006 |
| [余额核销账户关联](#oms_ar_balance_settlement) | `oms_ar_balance_settlement` | V007 |
| [余额批次使用明细](#oms_ar_balance_usage) | `oms_ar_balance_usage` | V007 |
| [发票关联](#oms_ar_invoice) | `oms_ar_invoice` | V008 |
| [发票分配](#oms_ar_invoice_allocation) | `oms_ar_invoice_allocation` | V008 |
| [汇率差异计算依据](#oms_ar_exchange_basis) | `oms_ar_exchange_basis` | V009 |
| [来源取消保护](#oms_ar_source_guard) | `oms_ar_source_guard` | V010 |
| [财务通知队列](#oms_ar_outbox) | `oms_ar_outbox` | V011 |
| [通知人工重试审计](#oms_ar_outbox_retry) | `oms_ar_outbox_retry` | V012 |

<a id="oms_ar_document"></a>

## 应收单头 · `oms_ar_document`

迁移来源 [V001__ar_core.sql](../oms-ar-server/src/main/resources/db/migration/V001__ar_core.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT` |
| `doc_no` | 应收单编号 | `VARCHAR(64)` | 否 | — | — |
| `customer_id` | 应收所属客户ID | `BIGINT` | 否 | — | — |
| `legal_entity_id` | 核算主体ID，也是权限范围 | `BIGINT` | 否 | — | — |
| `currency` | 结算币种代码 | `CHAR(3)` | 否 | — | — |
| `business_type` | 来源业务类别，例如出库或汇率差异 | `VARCHAR(32)` | 否 | — | — |
| `generation_mode` | 生成方式，区分直接录入与上游生成 | `VARCHAR(16)` | 否 | — | — |
| `business_date` | 业务发生日期 | `DATE` | 否 | — | — |
| `due_date` | 应收到期日期 | `DATE` | 是 | — | — |
| `status` | 记录所处的业务状态，允许值见本表 CHECK | `VARCHAR(16)` | 否 | `'ACTIVE'` | — |
| `version` | 用于并发校验的记录版本 | `BIGINT` | 否 | `0` | — |
| `void_reason` | 财务或上游作废原因 | `VARCHAR(500)` | 是 | — | — |
| `voided_by` | 执行整单作废的操作人 | `VARCHAR(100)` | 是 | — | — |
| `voided_time` | 整单作废完成时间 | `DATETIME(6)` | 是 | — | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_document_no(legal_entity_id,doc_no)`
- `UNIQUE KEY uk_document_scope(id,customer_id,legal_entity_id,currency)`
- `KEY ix_document_customer(legal_entity_id,customer_id,currency,status,due_date)`
- `CONSTRAINT ck_document_status CHECK(status IN ('ACTIVE','VOID'))`
- `CONSTRAINT ck_document_generation CHECK(generation_mode IN ('UPSTREAM','DIRECT'))`
- `CONSTRAINT ck_document_scope CHECK(customer_id>0 AND legal_entity_id>0)`
- `CONSTRAINT ck_document_version CHECK(version>=0)`

<a id="oms_ar_business_group"></a>

## 业务组 · `oms_ar_business_group`

迁移来源 [V001__ar_core.sql](../oms-ar-server/src/main/resources/db/migration/V001__ar_core.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT` |
| `group_no` | 业务组编号 | `VARCHAR(64)` | 否 | — | — |
| `customer_id` | 应收所属客户ID | `BIGINT` | 否 | — | — |
| `legal_entity_id` | 核算主体ID，也是权限范围 | `BIGINT` | 否 | — | — |
| `currency` | 结算币种代码 | `CHAR(3)` | 否 | — | — |
| `business_date` | 业务发生日期 | `DATE` | 否 | — | — |
| `version` | 用于并发校验的记录版本 | `BIGINT` | 否 | `0` | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_group_no(legal_entity_id,group_no)`
- `UNIQUE KEY uk_group_scope(id,customer_id,legal_entity_id,currency)`
- `CONSTRAINT ck_group_scope CHECK(customer_id>0 AND legal_entity_id>0)`
- `CONSTRAINT ck_group_version CHECK(version>=0)`

<a id="oms_ar_line"></a>

## 应收明细 · `oms_ar_line`

迁移来源 [V001__ar_core.sql](../oms-ar-server/src/main/resources/db/migration/V001__ar_core.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT` |
| `document_id` | 应收单ID | `BIGINT` | 否 | — | — |
| `group_id` | 业务组ID | `BIGINT` | 否 | — | — |
| `customer_id` | 应收所属客户ID | `BIGINT` | 否 | — | — |
| `legal_entity_id` | 核算主体ID，也是权限范围 | `BIGINT` | 否 | — | — |
| `currency` | 结算币种代码 | `CHAR(3)` | 否 | — | — |
| `line_no` | 应收单内的明细行序号 | `INT` | 否 | — | — |
| `amount_type` | 金额性质，区分价款和销项税 | `VARCHAR(16)` | 否 | — | — |
| `receivable_amount` | 带符号的原始应收金额 | `DECIMAL(24,8)` | 否 | — | — |
| `settled_amount` | 已核销金额，沿用原应收方向 | `DECIMAL(24,8)` | 否 | `0` | — |
| `transferred_amount` | 从负数应收转入信用账户的带符号金额 | `DECIMAL(24,8)` | 否 | `0` | — |
| `open_amount` | 当前未核销、未转出的应收余额 | `DECIMAL(24,8)` | 否 | — | — |
| `invoiced_amount` | 本行已登记的带符号开票金额 | `DECIMAL(24,8)` | 否 | `0` | — |
| `tax_rate` | 税率，以小数表示，空值与零税率区分 | `DECIMAL(12,8)` | 是 | — | — |
| `tax_base_amount` | 上游确认的计税依据金额 | `DECIMAL(24,8)` | 是 | — | — |
| `tax_method` | 计税方式 | `VARCHAR(32)` | 是 | — | — |
| `status` | 记录所处的业务状态，允许值见本表 CHECK | `VARCHAR(16)` | 否 | `'ACTIVE'` | — |
| `version` | 用于并发校验的记录版本 | `BIGINT` | 否 | `0` | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_line_number(document_id,line_no)`
- `KEY ix_line_group(group_id,status,id)`
- `CONSTRAINT fk_line_document FOREIGN KEY(document_id,customer_id,legal_entity_id,currency) REFERENCES oms_ar_document(id,customer_id,legal_entity_id,currency)`
- `CONSTRAINT fk_line_group FOREIGN KEY(group_id,customer_id,legal_entity_id,currency) REFERENCES oms_ar_business_group(id,customer_id,legal_entity_id,currency)`
- `CONSTRAINT ck_line_type CHECK(amount_type IN ('PRINCIPAL','OUTPUT_TAX'))`
- `CONSTRAINT ck_line_status CHECK(status IN ('ACTIVE','VOID'))`
- `CONSTRAINT ck_line_number CHECK(line_no>0)`
- `CONSTRAINT ck_line_version CHECK(version>=0)`
- `CONSTRAINT ck_line_tax_rate CHECK(tax_rate IS NULL OR tax_rate>=0)`
- `CONSTRAINT ck_line_original CHECK(receivable_amount<>0)`
- `CONSTRAINT ck_line_balance CHECK( (status='ACTIVE' AND open_amount=receivable_amount-settled_amount-transferred_amount) OR (status='VOID' AND open_amount=0 AND settled_amount=0 AND transferred_amount=0 AND invoiced_amount=0))`
- `CONSTRAINT ck_line_sign CHECK( (receivable_amount>0 AND settled_amount>=0 AND transferred_amount=0 AND open_amount>=0 AND settled_amount<=receivable_amount) OR (receivable_amount<0 AND settled_amount<=0 AND transferred_amount<=0 AND open_amount<=0 AND settled_amount+transferred_amount>=receivable_amount))`

<a id="oms_ar_line_source"></a>

## 应收明细来源 · `oms_ar_line_source`

迁移来源 [V001__ar_core.sql](../oms-ar-server/src/main/resources/db/migration/V001__ar_core.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT` |
| `line_id` | 本表记录所属的应收明细 ID | `BIGINT` | 否 | — | — |
| `source_system` | 来源系统标识 | `VARCHAR(64)` | 否 | — | — |
| `source_type` | 来源单据类型 | `VARCHAR(64)` | 否 | — | — |
| `source_no` | 来源单据编号 | `VARCHAR(128)` | 否 | — | — |
| `source_id` | 来源单据ID | `VARCHAR(128)` | 否 | — | — |
| `source_line_id` | 可选的上游单据行标识 | `VARCHAR(128)` | 是 | — | — |
| `source_event_id` | 来源系统的事件标识 | `VARCHAR(128)` | 否 | — | — |
| `source_output_key` | 同一来源事件内的输出标识 | `VARCHAR(64)` | 否 | — | — |
| `original_line_id` | 调整所指定的原正数应收行ID | `BIGINT` | 是 | — | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_source_line(line_id)`
- `UNIQUE KEY uk_source_event_output(source_system,source_event_id,source_output_key)`
- `KEY ix_source_document(source_system,source_type,source_id)`
- `CONSTRAINT fk_source_line FOREIGN KEY(line_id) REFERENCES oms_ar_line(id)`
- `CONSTRAINT fk_source_original FOREIGN KEY(original_line_id) REFERENCES oms_ar_line(id)`
- `CONSTRAINT ck_source_output CHECK(CHAR_LENGTH(source_output_key)>0)`
- `CONSTRAINT ck_source_event CHECK(CHAR_LENGTH(source_event_id)>0)`

<a id="oms_ar_operation_request"></a>

## 幂等操作请求 · `oms_ar_operation_request`

迁移来源 [V002__ar_audit.sql](../oms-ar-server/src/main/resources/db/migration/V002__ar_audit.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `legal_entity_id` | 核算主体ID，也是权限范围 | `BIGINT` | 否 | — | — |
| `caller_id` | 幂等请求所属调用方 | `VARCHAR(100)` | 否 | — | — |
| `operation_type` | 幂等操作的动作类型 | `VARCHAR(32)` | 否 | — | — |
| `request_key` | 调用方提供的幂等请求键 | `VARCHAR(128)` | 否 | — | — |
| `payload_hash` | 用于识别同键异内容请求的摘要 | `CHAR(64)` | 否 | — | — |
| `result_reference` | 操作已完成时可重放的结果引用 | `BIGINT` | 是 | — | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_operation_request(legal_entity_id,caller_id,operation_type,request_key)`
- `CONSTRAINT ck_operation_scope CHECK(legal_entity_id>0)`
- `CONSTRAINT ck_operation_key CHECK(CHAR_LENGTH(request_key)>0)`

<a id="oms_ar_lifecycle_event"></a>

## 生命周期事件 · `oms_ar_lifecycle_event`

迁移来源 [V002__ar_audit.sql](../oms-ar-server/src/main/resources/db/migration/V002__ar_audit.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `event_type` | 生命周期事件类型 | `VARCHAR(32)` | 否 | — | — |
| `occurred_at` | 业务事件发生时间 | `DATETIME(6)` | 否 | — | — |
| `recorded_at` | 事件写入系统的时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `actor` | 执行动作的可信操作人 | `VARCHAR(100)` | 否 | — | — |
| `reason` | 业务操作原因 | `VARCHAR(500)` | 是 | — | — |
| `trigger_type` | 触发事件的业务对象类型 | `VARCHAR(64)` | 是 | — | — |
| `trigger_id` | 触发事件的业务对象标识 | `VARCHAR(128)` | 是 | — | — |
| `correlation_id` | 串联同一业务操作的关联标识 | `VARCHAR(128)` | 否 | — | — |
| `reverse_of_id` | 撤销事件引用的原事件ID | `BIGINT` | 是 | — | — |
| `schema_version` | 快照结构版本 | `INT` | 否 | `1` | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `KEY ix_event_correlation(correlation_id)`
- `CONSTRAINT fk_event_reverse FOREIGN KEY(reverse_of_id) REFERENCES oms_ar_lifecycle_event(id)`
- `CONSTRAINT ck_event_schema CHECK(schema_version>0)`

<a id="oms_ar_lifecycle_change"></a>

## 对象变化快照 · `oms_ar_lifecycle_change`

迁移来源 [V002__ar_audit.sql](../oms-ar-server/src/main/resources/db/migration/V002__ar_audit.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `event_id` | 事件标识 | `BIGINT` | 否 | — | — |
| `object_type` | 被记录或诊断的对象类型 | `VARCHAR(32)` | 否 | — | — |
| `object_id` | 被记录或诊断的对象ID | `BIGINT` | 否 | — | — |
| `before_snapshot` | 变更前的对象快照，初始化时为空 | `JSON` | 是 | — | — |
| `delta_snapshot` | 本次变更的字段及增量 | `JSON` | 否 | — | — |
| `after_snapshot` | 变更后的对象快照 | `JSON` | 否 | — | — |
| `before_version` | 变更前的版本号 | `BIGINT` | 是 | — | — |
| `after_version` | 变更后的版本号 | `BIGINT` | 否 | — | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `KEY ix_change_object(object_type,object_id,event_id)`
- `CONSTRAINT fk_change_event FOREIGN KEY(event_id) REFERENCES oms_ar_lifecycle_event(id)`
- `CONSTRAINT ck_change_version CHECK(after_version>=0 AND (before_version IS NULL OR after_version>=before_version))`

<a id="oms_ar_receipt"></a>

## 实际收款 · `oms_ar_receipt`

迁移来源 [V003__ar_receipt.sql](../oms-ar-server/src/main/resources/db/migration/V003__ar_receipt.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `customer_id` | 应收所属客户ID | `BIGINT` | 否 | — | — |
| `legal_entity_id` | 核算主体ID，也是权限范围 | `BIGINT` | 否 | — | — |
| `currency` | 结算币种代码 | `CHAR(3)` | 否 | — | — |
| `amount` | 实收原额 | `DECIMAL(24,8)` | 否 | — | — |
| `allocated_amount` | 收款已用于核销的金额 | `DECIMAL(24,8)` | 否 | `0` | — |
| `version` | 用于并发校验的记录版本 | `BIGINT` | 否 | `0` | — |
| `received_date` | 实际收款日期 | `DATE` | 否 | — | — |
| `source_system` | 来源系统标识 | `VARCHAR(64)` | 否 | — | — |
| `source_id` | 来源单据ID | `VARCHAR(128)` | 否 | — | — |
| `source_no` | 来源单据编号 | `VARCHAR(128)` | 否 | — | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_receipt_source(legal_entity_id,source_system,source_id)`
- `UNIQUE KEY uk_receipt_scope(id,customer_id,legal_entity_id,currency)`
- `KEY ix_receipt_customer(legal_entity_id,customer_id,currency,id)`
- `CONSTRAINT ck_receipt_amount CHECK(amount>0 AND allocated_amount>=0 AND allocated_amount<=amount)`
- `CONSTRAINT ck_receipt_version CHECK(version>=0)`
- `CONSTRAINT ck_receipt_scope CHECK(customer_id>0 AND legal_entity_id>0)`

<a id="oms_ar_settlement"></a>

## 核销单 · `oms_ar_settlement`

迁移来源 [V004__ar_settlement.sql](../oms-ar-server/src/main/resources/db/migration/V004__ar_settlement.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `settlement_no` | 核销单编号 | `VARCHAR(64)` | 否 | — | — |
| `customer_id` | 应收所属客户ID | `BIGINT` | 否 | — | — |
| `legal_entity_id` | 核算主体ID，也是权限范围 | `BIGINT` | 否 | — | — |
| `currency` | 结算币种代码 | `CHAR(3)` | 否 | — | — |
| `settlement_type` | 核销资金类型，区分实收、负数抵销和余额抵扣 | `VARCHAR(16)` | 否 | — | — |
| `receipt_id` | 实际收款记录ID | `BIGINT` | 是 | — | — |
| `amount` | 本次确认的核销总额 | `DECIMAL(24,8)` | 否 | — | — |
| `strategy` | 用户选定的核销分配策略 | `VARCHAR(32)` | 否 | — | — |
| `preview_fingerprint` | 确认时记录的分配预览指纹 | `CHAR(64)` | 否 | — | — |
| `status` | 记录所处的业务状态，允许值见本表 CHECK | `VARCHAR(16)` | 否 | `'POSTED'` | — |
| `reason` | 业务操作原因 | `VARCHAR(500)` | 否 | — | — |
| `version` | 用于并发校验的记录版本 | `BIGINT` | 否 | `0` | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_settlement_no(legal_entity_id,settlement_no)`
- `KEY ix_settlement_receipt(receipt_id,status)`
- `CONSTRAINT fk_settlement_receipt FOREIGN KEY(receipt_id,customer_id,legal_entity_id,currency) REFERENCES oms_ar_receipt(id,customer_id,legal_entity_id,currency)`
- `CONSTRAINT ck_settlement_type CHECK(settlement_type IN ('RECEIPT','OFFSET','BALANCE'))`
- `CONSTRAINT ck_settlement_source CHECK((settlement_type='RECEIPT' AND receipt_id IS NOT NULL) OR (settlement_type IN ('OFFSET','BALANCE') AND receipt_id IS NULL))`
- `CONSTRAINT ck_settlement_amount CHECK(amount>0)`
- `CONSTRAINT ck_settlement_status CHECK(status IN ('POSTED','REVERSED'))`
- `CONSTRAINT ck_settlement_version CHECK(version>=0)`

<a id="oms_ar_settlement_allocation"></a>

## 核销分配 · `oms_ar_settlement_allocation`

迁移来源 [V004__ar_settlement.sql](../oms-ar-server/src/main/resources/db/migration/V004__ar_settlement.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `settlement_id` | 核销单ID | `BIGINT` | 否 | — | — |
| `sequence_no` | 分配或使用明细的顺序号 | `INT` | 否 | — | — |
| `positive_line_id` | 本次分配抵扣的正数应收行ID | `BIGINT` | 否 | — | — |
| `negative_line_id` | 提供抵销额度的负数应收行ID | `BIGINT` | 是 | — | — |
| `amount_type` | 金额性质，区分价款和销项税 | `VARCHAR(16)` | 否 | — | — |
| `amount` | 本次分配给目标正数行的金额 | `DECIMAL(24,8)` | 否 | — | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_allocation_sequence(settlement_id,sequence_no)`
- `KEY ix_allocation_positive(positive_line_id,settlement_id)`
- `KEY ix_allocation_negative(negative_line_id,settlement_id)`
- `CONSTRAINT fk_allocation_settlement FOREIGN KEY(settlement_id) REFERENCES oms_ar_settlement(id)`
- `CONSTRAINT fk_allocation_positive FOREIGN KEY(positive_line_id) REFERENCES oms_ar_line(id)`
- `CONSTRAINT fk_allocation_negative FOREIGN KEY(negative_line_id) REFERENCES oms_ar_line(id)`
- `CONSTRAINT ck_allocation_amount CHECK(amount>0)`
- `CONSTRAINT ck_allocation_type CHECK(amount_type IN ('PRINCIPAL','OUTPUT_TAX'))`
- `CONSTRAINT ck_allocation_sequence CHECK(sequence_no>0)`

<a id="oms_ar_adjustment_link"></a>

## 负数调整关联 · `oms_ar_adjustment_link`

迁移来源 [V005__ar_adjustment_link.sql](../oms-ar-server/src/main/resources/db/migration/V005__ar_adjustment_link.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `line_id` | 本表记录所属的应收明细 ID | `BIGINT` | 否 | — | — |
| `related_group_id` | 负数调整归属的原业务组ID | `BIGINT` | 否 | — | — |
| `original_line_id` | 调整所指定的原正数应收行ID | `BIGINT` | 是 | — | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_adjustment_line(line_id)`
- `KEY ix_adjustment_original(original_line_id)`
- `CONSTRAINT fk_adjustment_line FOREIGN KEY(line_id) REFERENCES oms_ar_line(id)`
- `CONSTRAINT fk_adjustment_group FOREIGN KEY(related_group_id) REFERENCES oms_ar_business_group(id)`
- `CONSTRAINT fk_adjustment_original FOREIGN KEY(original_line_id) REFERENCES oms_ar_line(id)`

<a id="oms_ar_customer_account"></a>

## 客户信用账户 · `oms_ar_customer_account`

迁移来源 [V006__ar_customer_balance.sql](../oms-ar-server/src/main/resources/db/migration/V006__ar_customer_balance.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `customer_id` | 应收所属客户ID | `BIGINT` | 否 | — | — |
| `legal_entity_id` | 核算主体ID，也是权限范围 | `BIGINT` | 否 | — | — |
| `currency` | 结算币种代码 | `CHAR(3)` | 否 | — | — |
| `balance_amount` | 客户信用账户可用余额 | `DECIMAL(24,8)` | 否 | `0` | — |
| `version` | 用于并发校验的记录版本 | `BIGINT` | 否 | `0` | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_account_scope(customer_id,legal_entity_id,currency)`
- `UNIQUE KEY uk_account_id_scope(id,customer_id,legal_entity_id,currency)`
- `CONSTRAINT ck_account_balance CHECK(balance_amount>=0)`
- `CONSTRAINT ck_account_version CHECK(version>=0)`

<a id="oms_ar_balance_lot"></a>

## 信用余额来源批次 · `oms_ar_balance_lot`

迁移来源 [V006__ar_customer_balance.sql](../oms-ar-server/src/main/resources/db/migration/V006__ar_customer_balance.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `account_id` | 客户信用账户ID | `BIGINT` | 否 | — | — |
| `source_line_id` | 本批次转入所引用的负数应收行 ID | `BIGINT` | 否 | — | — |
| `customer_id` | 应收所属客户ID | `BIGINT` | 否 | — | — |
| `legal_entity_id` | 核算主体ID，也是权限范围 | `BIGINT` | 否 | — | — |
| `currency` | 结算币种代码 | `CHAR(3)` | 否 | — | — |
| `amount` | 本批次转入的信用金额 | `DECIMAL(24,8)` | 否 | — | — |
| `used_amount` | 已占用或本次实际使用的金额 | `DECIMAL(24,8)` | 否 | `0` | — |
| `status` | 记录所处的业务状态，允许值见本表 CHECK | `VARCHAR(16)` | 否 | `'ACTIVE'` | — |
| `version` | 用于并发校验的记录版本 | `BIGINT` | 否 | `0` | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `KEY ix_lot_account(account_id,status,id)`
- `KEY ix_lot_source(source_line_id)`
- `CONSTRAINT fk_lot_account FOREIGN KEY(account_id,customer_id,legal_entity_id,currency) REFERENCES oms_ar_customer_account(id,customer_id,legal_entity_id,currency)`
- `CONSTRAINT fk_lot_source FOREIGN KEY(source_line_id) REFERENCES oms_ar_line(id)`
- `CONSTRAINT ck_lot_amount CHECK(amount>0 AND used_amount>=0 AND used_amount<=amount)`
- `CONSTRAINT ck_lot_status CHECK(status IN ('ACTIVE','REVERSED') AND (status='ACTIVE' OR used_amount=0))`
- `CONSTRAINT ck_lot_version CHECK(version>=0)`

<a id="oms_ar_balance_settlement"></a>

## 余额核销账户关联 · `oms_ar_balance_settlement`

迁移来源 [V007__ar_balance_settlement.sql](../oms-ar-server/src/main/resources/db/migration/V007__ar_balance_settlement.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `settlement_id` | 核销单ID | `BIGINT` | 否 | — | — |
| `account_id` | 客户信用账户ID | `BIGINT` | 否 | — | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_balance_settlement(settlement_id)`
- `KEY ix_balance_settlement_account(account_id,settlement_id)`
- `CONSTRAINT fk_bs_settlement FOREIGN KEY(settlement_id) REFERENCES oms_ar_settlement(id)`
- `CONSTRAINT fk_bs_account FOREIGN KEY(account_id) REFERENCES oms_ar_customer_account(id)`

<a id="oms_ar_balance_usage"></a>

## 余额批次使用明细 · `oms_ar_balance_usage`

迁移来源 [V007__ar_balance_settlement.sql](../oms-ar-server/src/main/resources/db/migration/V007__ar_balance_settlement.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `settlement_id` | 核销单ID | `BIGINT` | 否 | — | — |
| `allocation_id` | 业务分配记录ID | `BIGINT` | 否 | — | — |
| `lot_id` | 信用余额来源批次ID | `BIGINT` | 否 | — | — |
| `sequence_no` | 分配或使用明细的顺序号 | `INT` | 否 | — | — |
| `amount` | 本次从该批次使用的金额 | `DECIMAL(24,8)` | 否 | — | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_balance_usage_sequence(settlement_id,sequence_no)`
- `UNIQUE KEY uk_balance_usage_allocation_lot(allocation_id,lot_id)`
- `KEY ix_usage_lot(lot_id,settlement_id)`
- `CONSTRAINT fk_usage_settlement FOREIGN KEY(settlement_id) REFERENCES oms_ar_settlement(id)`
- `CONSTRAINT fk_usage_allocation FOREIGN KEY(allocation_id) REFERENCES oms_ar_settlement_allocation(id)`
- `CONSTRAINT fk_usage_lot FOREIGN KEY(lot_id) REFERENCES oms_ar_balance_lot(id)`
- `CONSTRAINT ck_usage_amount CHECK(amount>0 AND sequence_no>0)`

<a id="oms_ar_invoice"></a>

## 发票关联 · `oms_ar_invoice`

迁移来源 [V008__ar_invoice.sql](../oms-ar-server/src/main/resources/db/migration/V008__ar_invoice.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `source_system` | 来源系统标识 | `VARCHAR(64)` | 否 | — | — |
| `source_id` | 来源单据ID | `VARCHAR(128)` | 否 | — | — |
| `invoice_no` | 外部发票编号 | `VARCHAR(128)` | 否 | — | — |
| `invoice_date` | 外部发票日期 | `DATE` | 否 | — | — |
| `customer_id` | 应收所属客户ID | `BIGINT` | 否 | — | — |
| `legal_entity_id` | 核算主体ID，也是权限范围 | `BIGINT` | 否 | — | — |
| `currency` | 结算币种代码 | `CHAR(3)` | 否 | — | — |
| `invoice_kind` | 发票方向，蓝字或红字 | `VARCHAR(8)` | 否 | — | — |
| `original_invoice_id` | 红字引用的原蓝字关联ID | `BIGINT` | 是 | — | — |
| `total_amount` | 带符号的发票分配合计 | `DECIMAL(24,8)` | 否 | — | — |
| `status` | 记录所处的业务状态，允许值见本表 CHECK | `VARCHAR(16)` | 否 | `'ACTIVE'` | — |
| `version` | 用于并发校验的记录版本 | `BIGINT` | 否 | `0` | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_invoice_source(legal_entity_id,source_system,source_id)`
- `UNIQUE KEY uk_invoice_scope(id,customer_id,legal_entity_id,currency)`
- `UNIQUE KEY uk_invoice_direction(id,invoice_kind)`
- `UNIQUE KEY uk_invoice_original(id,original_invoice_id)`
- `KEY ix_invoice_customer(legal_entity_id,customer_id,currency,invoice_date,id)`
- `KEY ix_invoice_original(original_invoice_id,status)`
- `CONSTRAINT fk_invoice_original_scope FOREIGN KEY(original_invoice_id,customer_id,legal_entity_id,currency) REFERENCES oms_ar_invoice(id,customer_id,legal_entity_id,currency)`
- `CONSTRAINT ck_invoice_kind CHECK((invoice_kind='BLUE' AND original_invoice_id IS NULL AND total_amount>0) OR (invoice_kind='RED' AND original_invoice_id IS NOT NULL AND total_amount<0))`
- `CONSTRAINT ck_invoice_status CHECK(status IN ('ACTIVE','REVERSED'))`
- `CONSTRAINT ck_invoice_version CHECK(version>=0)`

<a id="oms_ar_invoice_allocation"></a>

## 发票分配 · `oms_ar_invoice_allocation`

迁移来源 [V008__ar_invoice.sql](../oms-ar-server/src/main/resources/db/migration/V008__ar_invoice.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `invoice_id` | 发票关联记录ID | `BIGINT` | 否 | — | — |
| `invoice_kind` | 发票方向，蓝字或红字 | `VARCHAR(8)` | 否 | — | — |
| `invoice_line_id` | 外部发票行标识 | `VARCHAR(128)` | 否 | — | — |
| `receivable_line_id` | 分配所关联的应收行ID | `BIGINT` | 否 | — | — |
| `amount_type` | 金额性质，区分价款和销项税 | `VARCHAR(16)` | 否 | — | — |
| `amount` | 本次关联金额，蓝字为正、红字为负 | `DECIMAL(24,8)` | 否 | — | — |
| `tax_rate` | 税率，以小数表示，空值与零税率区分 | `DECIMAL(12,8)` | 是 | — | — |
| `original_invoice_id` | 红字引用的原蓝字关联ID | `BIGINT` | 是 | — | — |
| `original_allocation_id` | 红字分配引用的原蓝字分配ID | `BIGINT` | 是 | — | — |
| `original_allocation_key` | 去重用生成列，蓝字取 0，红字取原蓝字分配 ID | `BIGINT` | 是 | — | `GENERATED ALWAYS AS (COALESCE(original_allocation_id,0)) STORED` |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_invoice_line_receivable(invoice_id,invoice_line_id,receivable_line_id,original_allocation_key)`
- `UNIQUE KEY uk_invoice_allocation_original(id,invoice_id,amount_type)`
- `KEY ix_invoice_allocation_receivable(receivable_line_id,invoice_id)`
- `KEY ix_invoice_allocation_red(original_allocation_id)`
- `CONSTRAINT fk_invoice_allocation_direction FOREIGN KEY(invoice_id,invoice_kind) REFERENCES oms_ar_invoice(id,invoice_kind)`
- `CONSTRAINT fk_invoice_allocation_header_original FOREIGN KEY(invoice_id,original_invoice_id) REFERENCES oms_ar_invoice(id,original_invoice_id)`
- `CONSTRAINT fk_invoice_allocation_receivable FOREIGN KEY(receivable_line_id) REFERENCES oms_ar_line(id)`
- `CONSTRAINT fk_invoice_allocation_original FOREIGN KEY(original_allocation_id,original_invoice_id,amount_type) REFERENCES oms_ar_invoice_allocation(id,invoice_id,amount_type)`
- `CONSTRAINT ck_invoice_allocation_type CHECK(amount_type IN ('PRINCIPAL','OUTPUT_TAX'))`
- `CONSTRAINT ck_invoice_allocation_rate CHECK((amount_type='PRINCIPAL' AND (tax_rate IS NULL OR tax_rate>=0)) OR (amount_type='OUTPUT_TAX' AND tax_rate IS NOT NULL AND tax_rate>=0))`
- `CONSTRAINT ck_invoice_allocation_direction CHECK((invoice_kind='BLUE' AND amount>0 AND original_invoice_id IS NULL AND original_allocation_id IS NULL) OR (invoice_kind='RED' AND amount<0 AND original_invoice_id IS NOT NULL AND original_allocation_id IS NOT NULL))`

<a id="oms_ar_exchange_basis"></a>

## 汇率差异计算依据 · `oms_ar_exchange_basis`

迁移来源 [V009__ar_exchange_basis.sql](../oms-ar-server/src/main/resources/db/migration/V009__ar_exchange_basis.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `line_id` | 本表记录所属的应收明细 ID | `BIGINT` | 否 | — | — |
| `original_currency` | 折算前的原币种 | `CHAR(3)` | 否 | — | — |
| `original_amount` | 原始金额 | `DECIMAL(24,8)` | 否 | — | — |
| `old_rate` | 原换算汇率 | `DECIMAL(24,12)` | 否 | — | — |
| `new_rate` | 新的换算汇率 | `DECIMAL(24,12)` | 否 | — | — |
| `rate_direction` | 汇率报价方向，决定乘法或除法折算 | `VARCHAR(32)` | 否 | — | — |
| `old_settlement_amount` | 按原汇率计算的结算币金额 | `DECIMAL(24,8)` | 否 | — | — |
| `new_settlement_amount` | 按新汇率计算的结算币金额 | `DECIMAL(24,8)` | 否 | — | — |
| `confirmed_difference` | 以结算币种确认的汇率差额 | `DECIMAL(24,8)` | 否 | — | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_exchange_line(line_id)`
- `CONSTRAINT fk_exchange_line FOREIGN KEY(line_id) REFERENCES oms_ar_line(id)`
- `CONSTRAINT ck_exchange_rates CHECK(old_rate>0 AND new_rate>0)`
- `CONSTRAINT ck_exchange_original CHECK(original_amount<>0)`
- `CONSTRAINT ck_exchange_direction CHECK(rate_direction IN ('SETTLEMENT_PER_ORIGINAL','ORIGINAL_PER_SETTLEMENT'))`
- `CONSTRAINT ck_exchange_difference CHECK(confirmed_difference<>0 AND confirmed_difference=new_settlement_amount-old_settlement_amount)`

<a id="oms_ar_source_guard"></a>

## 来源取消保护 · `oms_ar_source_guard`

迁移来源 [V010__ar_source_guard.sql](../oms-ar-server/src/main/resources/db/migration/V010__ar_source_guard.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT` |
| `legal_entity_id` | 核算主体ID，也是权限范围 | `BIGINT` | 否 | — | — |
| `source_key` | 稳定来源身份的摘要键 | `CHAR(64)` | 否 | — | `CHARACTER SET ascii COLLATE ascii_bin` |
| `source_system` | 来源系统标识 | `VARCHAR(64)` | 否 | — | — |
| `source_type` | 来源单据类型 | `VARCHAR(64)` | 否 | — | — |
| `source_id` | 来源单据ID | `VARCHAR(128)` | 否 | — | — |
| `source_line_id` | 可选的上游单据行标识 | `VARCHAR(128)` | 是 | — | — |
| `blocked` | 来源是否已禁止再次生成应收 | `BOOLEAN` | 否 | `FALSE` | — |
| `version` | 用于并发校验的记录版本 | `BIGINT` | 否 | `0` | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_source_guard(legal_entity_id,source_key)`
- `KEY ix_source_guard_document(legal_entity_id,source_system,source_type,source_id)`
- `CONSTRAINT ck_source_guard_scope CHECK(legal_entity_id>0)`
- `CONSTRAINT ck_source_guard_version CHECK(version>=0)`

<a id="oms_ar_outbox"></a>

## 财务通知队列 · `oms_ar_outbox`

迁移来源 [V011__ar_outbox.sql](../oms-ar-server/src/main/resources/db/migration/V011__ar_outbox.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `event_id` | 事件标识 | `BIGINT` | 否 | — | — |
| `legal_entity_id` | 核算主体ID，也是权限范围 | `BIGINT` | 否 | — | — |
| `destination` | 通知的逻辑投递目的地 | `VARCHAR(64)` | 否 | — | — |
| `payload` | 投递给接收方的完整事件及变化明细 | `JSON` | 否 | — | — |
| `status` | 记录所处的业务状态，允许值见本表 CHECK | `VARCHAR(16)` | 否 | `'PENDING'` | — |
| `attempt_count` | 当前投递轮次已尝试的次数 | `INT` | 否 | `0` | — |
| `next_attempt_at` | 下次允许领取通知的时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `lease_token` | 派发工作者本次领取的凭证 | `VARCHAR(36)` | 是 | — | — |
| `lease_until` | 派发租约到期时间 | `DATETIME(6)` | 是 | — | — |
| `sent_time` | 成功投递完成时间 | `DATETIME(6)` | 是 | — | — |
| `last_error` | 最近一次投递错误分类 | `VARCHAR(500)` | 是 | — | — |
| `version` | 用于并发校验的记录版本 | `BIGINT` | 否 | `0` | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | `CURRENT_TIMESTAMP(6)` | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_outbox_event_destination(event_id,destination)`
- `KEY ix_outbox_ready(status,next_attempt_at,id)`
- `KEY ix_outbox_lease(status,lease_until,id)`
- `KEY ix_outbox_scope(legal_entity_id,id)`
- `CONSTRAINT fk_outbox_event FOREIGN KEY(event_id) REFERENCES oms_ar_lifecycle_event(id)`
- `CONSTRAINT ck_outbox_scope CHECK(legal_entity_id>0 AND CHAR_LENGTH(destination)>0)`
- `CONSTRAINT ck_outbox_count CHECK(attempt_count>=0 AND version>=0)`
- `CONSTRAINT ck_outbox_status CHECK(status IN ('PENDING','PROCESSING','RETRY','SENT','DEAD'))`
- `CONSTRAINT ck_outbox_lease CHECK((status='PROCESSING' AND lease_token IS NOT NULL AND CHAR_LENGTH(lease_token)=36 AND lease_until IS NOT NULL) OR (status<>'PROCESSING' AND lease_token IS NULL AND lease_until IS NULL))`
- `CONSTRAINT ck_outbox_sent CHECK((status='SENT' AND sent_time IS NOT NULL) OR (status<>'SENT' AND sent_time IS NULL))`

<a id="oms_ar_outbox_retry"></a>

## 通知人工重试审计 · `oms_ar_outbox_retry`

迁移来源 [V012__ar_outbox_retry.sql](../oms-ar-server/src/main/resources/db/migration/V012__ar_outbox_retry.sql)。

| 字段 | 中文含义 | MySQL 类型 | 允许 NULL | 显式默认值 | 附加定义 |
| --- | --- | --- | --- | --- | --- |
| `id` | 本表自增主键 | `BIGINT` | 否 | — | `AUTO_INCREMENT PRIMARY KEY` |
| `legal_entity_id` | 核算主体ID，也是权限范围 | `BIGINT` | 否 | — | — |
| `outbox_id` | 待发送通知记录ID | `BIGINT` | 否 | — | — |
| `operation_id` | 幂等操作记录ID | `BIGINT` | 否 | — | — |
| `reason` | 业务操作原因 | `VARCHAR(500)` | 否 | — | — |
| `before_status` | 变更前的状态 | `VARCHAR(16)` | 否 | — | — |
| `before_attempt_count` | 重试前的尝试次数 | `INT` | 否 | — | — |
| `before_next_attempt_at` | 重试前的下次调度时间 | `DATETIME(6)` | 否 | — | — |
| `before_last_error` | 重试前最后一次派发错误 | `VARCHAR(500)` | 是 | — | — |
| `before_version` | 变更前的版本号 | `BIGINT` | 否 | — | — |
| `after_status` | 变更后的状态 | `VARCHAR(16)` | 否 | — | — |
| `after_attempt_count` | 重试后的尝试次数 | `INT` | 否 | — | — |
| `after_next_attempt_at` | 重试后的下次调度时间 | `DATETIME(6)` | 否 | — | — |
| `after_version` | 变更后的版本号 | `BIGINT` | 否 | — | — |
| `created_time` | 记录创建时间 | `DATETIME(6)` | 否 | — | — |
| `created_by` | 创建记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `updated_time` | 记录最后更新时间 | `DATETIME(6)` | 否 | — | — |
| `updated_by` | 最近更新记录的操作人 | `VARCHAR(100)` | 否 | — | — |
| `delete_status` | 记录的逻辑删除标志 | `BOOLEAN` | 否 | `FALSE` | — |

**索引与约束**

- `PRIMARY KEY(id)`
- `UNIQUE KEY uk_outbox_retry_operation(operation_id)`
- `UNIQUE KEY uk_outbox_retry_version(outbox_id,before_version)`
- `KEY ix_outbox_retry_scope(legal_entity_id,outbox_id,id)`
- `CONSTRAINT fk_outbox_retry_outbox FOREIGN KEY(outbox_id) REFERENCES oms_ar_outbox(id)`
- `CONSTRAINT fk_outbox_retry_operation FOREIGN KEY(operation_id) REFERENCES oms_ar_operation_request(id)`
- `CONSTRAINT ck_outbox_retry_scope CHECK(legal_entity_id>0 AND CHAR_LENGTH(TRIM(reason))>0)`
- `CONSTRAINT ck_outbox_retry_state CHECK(before_status IN ('RETRY','DEAD') AND after_status='PENDING' AND before_attempt_count>=0 AND after_attempt_count=0)`
- `CONSTRAINT ck_outbox_retry_version CHECK(before_version>=0 AND after_version=before_version+1)`
