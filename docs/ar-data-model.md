# MySQL 8.0.42 应收逻辑表设计

本文为逻辑模型，不是已执行的DDL。表名前缀 oms_ar_；所有主键 BIGINT 自增。金额建议 DECIMAL(24,8)，税率 DECIMAL(12,8)，汇率 DECIMAL(24,12)，业务金额入账须按币种精度校验。0税率与税率不适用NULL区分。

## 表清单

| 表 | 主要字段与约束 |
| --- | --- |
| document | doc_no、customer_id、legal_entity_id、currency、business_type、generation_mode、business_date、due_date、status、version；主体内编号唯一 |
| business_group | group_no、customer_id、legal_entity_id、currency、business_date；跨单关联但禁止跨客户/主体/币种 |
| line | document_id、group_id、line_no、amount_type、receivable_amount、settled_amount、transferred_amount、open_amount、invoiced_amount、tax_rate、tax_base_amount、status、version；单据内行号唯一 |
| line_source | line_id、source_system、source_type、source_no、source_id、source_line_id、source_event_id、source_output_key、original_line_id；source_output_key不为空并与来源事件联合唯一 |
| exchange_basis | line_id、original_currency、original_amount、old_rate、new_rate、rate_direction、confirmed_difference；保留原计算快照 |
| settlement | settlement_no、settlement_type、customer_id、legal_entity_id、currency、strategy、status、request_key、reverse_of_id |
| settlement_allocation | settlement_id、sequence_no、positive_line_id、negative_line_id、receipt_reference、balance_lot_id、amount；amount为正值，按类型限制来源字段组合 |
| balance_account | customer_id、legal_entity_id、currency、available_amount、version；三元组唯一，余额不可负 |
| balance_transfer | source_line_id、account_id、amount、status、request_key、reverse_of_id；金额为正，原应收转出记负方向 |
| balance_lot | account_id、transfer_id、original_amount、remaining_amount、version；每笔转入保留来源 |
| balance_entry | account_id、lot_id、operation_id、delta_amount、before_amount、after_amount；只追加 |
| invoice_allocation | invoice_id、invoice_line_id、receivable_line_id、amount、tax_rate、event_id、reverse_of_id；红冲保留原关联 |
| lifecycle_event | event_type、occurred_at、recorded_at、actor、reason、trigger_type、trigger_id、correlation_id、reverse_of_id、schema_version；只追加 |
| lifecycle_change | event_id、object_type、object_id、before_snapshot、delta_snapshot、after_snapshot、before_version、after_version；快照为JSON |
| operation_request | legal_entity_id、caller_id、operation_type、request_key、payload_hash、result_reference；前四项联合唯一 |
| outbox | event_id、destination、payload、delivery_status、attempt_count、next_attempt_at；与业务事务一起插入 |

系统字段按指南保存 id、created_time、created_by、updated_time、updated_by、delete_status；业务作废不能通过delete_status隐藏。审计表不提供更新删除业务接口，状态变更写新事件。outbox的投递状态是可变运行数据，不作为不可变审计流水。

## 不变量

- 生效行 open = receivable - settled - transferred。正负各按原方向记录，不能核销越过零。
- 负100抵销60、转余额30：settled=-60，transferred=-30，open=-10；账户增加30。
- 作废保留原始金额，活动余额归零，以状态和作废事件解释，不伪造核销。
- 单头汇总按金额类型查询；结清要求所有有效行余额为零，禁止以净额为零替代。
- 开票净额不参与结算余额公式。跨类型资金抵销不改变计税依据。
- 零金额原始应收不生成空行；币种精度以内的尾差必须明确归属，禁止静默超额。

## 索引与并发

line 建 document_id/line_no 唯一索引、group_id/status/id 查询索引；document 建主体/客户/币种/状态/到期日查询索引；分配表为双方应收ID建追溯索引；事件变化为object_type/object_id/event_id建索引。

来源幂等不依赖可空source_line_id，避免无行ID时重复生成。请求唯一性与业务行版本均需数据库约束兜底。金额更新使用行锁或version条件更新，零行更新视为冲突并回滚。使用InnoDB；DDL正式编写时逐项实现外键、CHECK和唯一约束并以MySQL实库用例验证。

## 已落地的余额结构（V006–V007）

逻辑模型中的账户落地为 `customer_account`，余额字段为 `balance_amount`。每次转入直接生成 `balance_lot`，保存 `source_line_id`、原额 `amount`、已使用额 `used_amount`、`status` 和版本，不另建同义的转入单表。转入/撤销的操作键与逆向关联分别由 `operation_request`、`lifecycle_event` 管理。

余额抵扣使用既有 `settlement`（类型BALANCE）及 `settlement_allocation` 保存应收分配；`balance_settlement` 记录账户，`balance_usage` 记录每条应收分配对应的来源批次和金额。一条应收分配可以使用多个来源批次，因此批次ID没有直接放在应收分配表上。收款来源保存在收款核销单的 `receipt_id`，正负抵销来源保存在分配的 `negative_line_id`。

账户和批次的前额、变动额、后额及版本写入共用 `lifecycle_change`，不另建重复的 `balance_entry`。批次和分配记录均保留，撤销由核销/批次状态及逆向事件表达。

守恒条件：账户余额等于所有ACTIVE批次的 `amount-used_amount` 合计；一笔余额核销的每条应收分配金额等于其批次使用明细合计，全部使用明细合计等于核销单金额。反核销恢复原批次，不重新运行FIFO。原应收的 `transferred_amount` 只在转入及撤销转入时变化，余额后续抵扣不重复修改该来源应收。

## 开票关联存储（V008，服务已实现）

`invoice`保存外部来源系统/ID、发票编号/日期、客户/主体/币种、BLUE或RED方向、原发票引用、合计与状态；来源按主体唯一。`invoice_allocation`保存外部发票行到应收行的分配，金额字段按方向带符号，价税通过amount_type区分，税率为该次关联快照。红字分配通过原发票与原分配ID追溯，不能引用其他原发票或另一价税类型的分配。

数据库约束覆盖蓝正红负、原关联必填、同范围原发票、分配方向一致、税行税率必填、来源唯一和同一发票行/应收行/原分配组合唯一。额度累计、原发票必须为有效蓝字、源应收范围/类型/税率一致、红字不超过原分配可红额度、单头明细合计一致、有效红字依赖及开票调整链已由开票关联服务在锁定事务内校验，详见HTTP接口契约和任务8b2/8c验证记录。关联撤销保留原记录，通过状态及共享生命周期事件解释。

红字合并行允许同一负数应收分别对应多条原发票分配；唯一键使用生成列original_allocation_key（原分配ID，蓝字为0），既避免蓝字NULL绕过唯一校验，也避免误拦合法红字合并。该生成列不由业务服务写入。


## 稳定来源身份锁（V010）

`source_guard`按主体和稳定来源键唯一。键是来源system/type/id/lineId的JSON数组SHA256，不含事件编号或输出键；原始身份字段同时保存并核对，NULL与空行号不同。价税输出共享来源锁，不同主体和来源行隔离。来源没有行号时使用单据级槽，不自动代表所有具体行。

创建幂等重放先返回原记录；新请求按键排序，在所有业务组锁之前初始化并锁定来源身份。被标记blocked的来源拒绝新事件重新生成；未blocked不代替原有事件/输出唯一键，也不禁止上游合法的不同事件。初始化身份快照与应收创建同事务。来源锁记录不删除，不提供自行解除拦截入口。

任务9b已实现财务及上游作废设置blocked、权限和生命周期事件，包含迟到生成与取消竞争验证；接口边界见http-api.md。

## 通知存储基础（V011）

outbox保存事件ID、主体、逻辑目的地及JSON payload，事件/目的地唯一。状态为PENDING/PROCESSING/RETRY/SENT/DEAD；attempt_count、next_attempt_at、lease_token/lease_until、sent_time、last_error和version属于可变投递运行状态。只有PROCESSING有租约，只有SENT有sent_time；事件外键保留来源。payload不可变要求由后续服务保证，DDL不宣称已经实现派发或重试。当前仅存储基础完成，业务事务入队与后台投递仍待实现。
