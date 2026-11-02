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

outbox保存事件ID、主体、逻辑目的地及JSON payload，事件/目的地唯一。状态为PENDING/PROCESSING/RETRY/SENT/DEAD；attempt_count、next_attempt_at、lease_token/lease_until、sent_time、last_error和version属于可变投递运行状态。只有PROCESSING有租约，只有SENT有sent_time；事件外键保留来源。业务创建路径与FinanceJournal现已通过OutboxEnqueueService在同一事务保存完整payload；没有修改payload的业务入口。后台投递与重试仍待实现。


## 通知事务入队与精确快照

创建应收、收款、核销及反核销、余额转入及撤销、开票及撤销、作废均在提交前入队，失败回滚整个业务事务。已完成请求重放返回原结果，不重复创建事件或通知。主体来自创建请求的授权结果或JournalTicket，不从快照文本猜测。逻辑目的地固定FINANCE_EVENTS，代表该主体的可信财务集成端，可包含同主体多个客户的完整变化，不能作为按客户过滤的外发订阅使用。

payload外层schemaVersion=1，包含eventId、legalEntityId、destination、完整event及按变化ID排序的changes；before/delta/afterSnapshot均为JSON结构，保留对象ID、前后版本、反向事件等追溯字段。当前仅保存队列，没有外发网络调用。后续发送须直接使用存储payload，不按最新业务数据重建。

新事件event.schemaVersion=2：快照中的小数以精确十进制文本保存（包括金额、税率和汇率），整数仍可为JSON数值；读取方须使用十进制解析，同时兼容整数数值。这样避免MySQL JSON将大额小数转为二进制浮点而舍入。业务DECIMAL列、普通业务接口金额类型和幂等请求摘要不变。历史schemaVersion=1流水保持原样，不尝试凭当前余额重建历史精度；旧流水中已发生的舍入无法由此修复。对账JSON_VALUE(... RETURNING DECIMAL)兼容两种表示。


## 通知租约与失败状态

后台内部服务按主体及FINANCE_EVENTS领取到期的PENDING/RETRY或租约已过期的PROCESSING记录，过滤已删除数据。数据库时钟NOW(6)、ID顺序和FOR UPDATE SKIP LOCKED用于短事务领取；每次实际取得租约时attempt_count加一，包含之后进程崩溃的尝试。达到次数上限的到期记录进入DEAD，不再自动发送。若一次调用只清理了耗尽记录，可以返回空；调度器下一轮继续处理后续队列。

领取和结果确认分别为REQUIRES_NEW/READ_COMMITTED事务。固定SQL用于数据库时间、行锁及CAS，属于DAO规范的明确扩展；DAO本身不开事务。确认必须同时匹配ID、主体、目的地、PROCESSING、租约令牌及未过期时间。过期工作者即使尚无人重领也不能确认。确认成功清空租约；成功保存sent_time，失败按尝试次数进入RETRY或DEAD。仅保存HTTP状态或预定义错误分类，不保存远端正文、异常详情或凭据。通知payload与财务余额不变。

租约只能约束本地状态，不能撤回远端已经处理的请求；接收端仍必须按eventId+destination去重。下一步发送器须在事务外发出请求、限制总超时，并使用超过请求超时的租约。当前租约服务没有HTTP映射，也未开启调度或外发。


## 通知发送器（默认关闭）

OutboxDispatchService每轮对每个已配置主体最多领取一条，HTTP在NOT_SUPPORTED方法中执行，领取与确认各自短事务。服务端配置固定HTTPS接收地址，不接受业务请求传入地址，也不跟随3xx重定向。仅显式allow-loopback-http=true时允许本机localhost/127.0.0.1/::1的HTTP，用于本机模拟。URL禁止用户信息、查询串和片段；Bearer凭据只能通过受保护服务端配置加载，不存入队列、VO或日志。运行中不提供配置修改接口，变更接收端需重启服务。

直接发送已存储payload，Content-Type为application/json，Idempotency-Key为ar-{legalEntityId}-{eventId}-{destination}，X-AR-Event-Id为事件ID。接收端必须先持久化处理或去重结果再返回2xx；任一2xx视为成功。3xx/4xx/5xx、网络错误、超时均进入有次数上限的重试。总超时覆盖连接/响应头/完整正文，超时取消请求；租约至少比请求超时长5秒。只记录HTTP状态及TIMEOUT/IO_ERROR/INTERRUPTED等分类，不保存响应正文或异常详情。

默认请求总超时3000毫秒、租约30秒、最大8次、退避5秒倍增至3600秒。退避为min(base*2^(attempt-1),max)，领取耗尽后DEAD。租约或确认失败可能导致重复投递，接收端须按幂等键去重；不保证跨事件顺序或跨系统严格一次，跨实例多个工作者可并发处理。单轮每主体一条、固定延迟执行，不承诺高吞吐；需要更高吞吐可部署多个工作者。

只有ar.outbox.enabled与ar.outbox.scheduling-enabled都为true才注册自动调度；缺少接收端配置的主体保持队列待处理。默认两个开关均false，本项目未配置任何真实接收端。一次调用最多遍历100个主体，各自失败不会阻断其他主体，失败日志仅带主体/事件标识。默认轮间延迟5000毫秒，可配置100至600000毫秒。


调度时间口径修正：初次入队next_attempt_at由数据库CURRENT_TIMESTAMP(6)默认填充，不使用应用本地时间；自动重试、租约截止和成功时间同样使用数据库时钟。新增实库验证从业务创建到立即领取的完整路径，防止应用与数据库时区不一致造成初次发送延迟。旧版本已写入的时间不自动改写。


## 人工重试审计（V012）

oms_ar_outbox_retry通过outbox_id和operation_id外键关联原通知与幂等请求；operation_id唯一、(outbox_id,before_version)唯一，保证每个操作/每个前版本仅一条审计。记录主体、原因、前后状态、尝试数、调度时间、错误、版本及六个审计字段。CHECK约束前状态RETRY/DEAD、后状态PENDING、后尝试数0、版本恰加一。

人工重试按幂等请求锁→通知行锁顺序，在READ_COMMITTED事务内校验权限/原因/版本/状态，CAS重置运行状态，读取数据库生成的next_attempt_at后保存审计与幂等结果。查询历史使用REPEATABLE_READ一致快照，先确认通知属于授权主体且未删除，再用相同范围过滤计数和记录。该运营审计不进入财务事件通知队列，避免递归外发；不更改应收、收款、核销或开票金额。
