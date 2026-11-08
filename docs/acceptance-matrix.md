# 应收后端需求验收矩阵

核心应收 V012 核对日期为 2026-11-03，实现基线为 2ed3dd8。现金分户 V020 的验证记录见本页末节。此表记录实现及验证证据；最终独立分支评审已完成，无已确认待修复问题。本期本机后端交付完成，不代表生产发布就绪。

Java测试位于oms-ar-server/src/test/java/com/punjab/oms/ar；业务实现位于同模块src/main/java/com/punjab/oms/ar/service/finance/impl。以下类名可直接定位源码；实库用例必须通过scripts/test-mysql-all启用独立测试库门禁。

| 已确认要求 | 实现入口 | 关键验证证据 |
| --- | --- | --- |
| 上游或直接生成，成功即有可核销应收；价款和销项税分行，金额字段按类型共用 | ReceivableServiceImpl；ArLine | ReceivableCreateRealTest.createAndReplay、queryDetail；ArSchemaRealTest.amountConstraints |
| 应收、已核销、转出、未核销、已开票分别维护；净额零不视为结清 | ReceivableQueryServiceImpl；SettlementServiceImpl | ReceivableCreateRealTest.netZeroNotSettled、settlementStates；ArSchemaRealTest.invoicingIndependent |
| 独立税额组、跨单关联、结清后补税 | ReceivableServiceImpl | ReceivableCreateRealTest.appendTax；AllocationServiceImplTest.zeroAndTaxOnly |
| 按用户确认的组顺序，组内先税后价；预留另一策略 | AllocationServiceImpl；SettlementServiceImpl | AllocationServiceImplTest.allocatesByGroup、partialTax、reservedStrategy；SettlementRealTest.confirmAndReverse |
| 关联负数优先原行，同类型续抵需要显式确认；独立负数按原顺序可跨类型 | OffsetServiceImpl | OffsetRealTest（10项）；HTTP抵销链路；不修改负数原有金额类型 |
| 负数剩余可以待核销、转余额或继续抵销，操作分别由调用方确认 | OffsetServiceImpl；CustomerBalanceServiceImpl | OffsetRealTest.explicitContinuation；CustomerBalanceRealTest.afterOffset、transferReverse；预览不占款 |
| 客户余额按客户、主体、币种隔离；来源批次、FIFO使用、撤销依赖 | CustomerBalanceServiceImpl；BalanceSettlementServiceImpl | CustomerBalanceRealTest（8项）；BalanceSettlementRealTest（10项），包括并发及流水失败回滚 |
| 开票差异先生成调整应收；原税率不可由开票修改；关联负数按原业务组、类型、税率影响额度 | ReceivableServiceImpl；InvoiceEligibilityServiceImpl；InvoiceServiceImpl | InvoiceEligibilityRealTest.changedRate、mismatchedOriginalRate、crossGroup、transferred、settlementIndependent；InvoiceRealTest |
| 蓝红字开票关联及撤销，额度不随实收/核销变化 | InvoiceServiceImpl | InvoiceRealTest（19项），共享额度并发、红蓝依赖、过期预览、回滚；HTTP蓝红字链路 |
| 汇率差异以结算币种计入实际价款应收，保留计算依据 | ReceivableServiceImpl；InvoiceEligibilityServiceImpl | ExchangeBasisRealTest（9项）；InvoiceEligibilityRealTest.exchangePolicy；开票默认关闭 |
| 来源类型、编号、ID、可选行ID；来源防重及作废后禁止隐式重新生成 | ReceivableServiceImpl；ReceivableVoidServiceImpl | ArSchemaRealTest.sourceWithoutLineIdIsUnique；SourceGuardRealTest（7项）；VoidRealTest.cancelBeforeCreate |
| 财务有权限填写原因即可作废，上游也可作废；先解除有效核销、转入与开票依赖 | ReceivableVoidServiceImpl | VoidRealTest（16项），部分来源取消、权限/原因、依赖、并发、流水失败回滚 |
| 生命周期初始化快照、业务事件及金额/税率前后变化完整保留 | FinanceJournalServiceImpl；LifecycleServiceImpl | LifecycleRealTest；创建、核销、余额、开票、作废回滚测试；OutboxEnqueueRealTest精度测试 |
| 写动作幂等、主体权限隔离、确认重验余额/版本，反向动作按原分配恢复 | FinanceJournalServiceImpl；ApiAuthenticationFilter；各业务服务 | CurrentUserTest、ApiAuthenticationFilterTest、ReceivableHttpRealTest及各业务并发/重放用例 |
| 明细查询、生命周期查询与内部对账 | ReceivableQueryServiceImpl；FinancePageServiceImpl；ReconciliationServiceImpl | ReceivableCreateRealTest查询用例；LifecycleRealTest；ReconciliationRealTest（22项） |
| 业务同事务保存待通知事件，可靠重试、人工重试留痕 | OutboxEnqueue/Lease/Dispatch/Query/RetryServiceImpl | 对应Outbox*RealTest；OutboxHttpTest、OutboxRetryHttpTest；默认关闭外发 |
| MySQL8.0.42、Java21，从零工程，无Nacos；迁移与启动可验证 | 正式db/migration；SchemaContractVerifier；scripts/run-local | ConfigurationBoundaryTest；MigrationResourcesTest；MigrationRealTest（5项）；SchemaContractRealTest（7项）；test-application-startup |
| Git初始化，每细分任务单独提交 | feature/ar-foundation | Git历史与docs/progress.md；未推送、合并或发布 |

## 验证记录

2026-11-03的scripts/verify-local已全部通过：Python启动配置5项、Java默认37项、业务实库201项、独立迁移/结构12项及打包应用5组验收。复跑记录work/final-acceptance-verification.log（05:55完成），历史记录work/local-verification-final.log；启动报告work/startup-smoke-result.json确认临时数据库已清理且未发送外部通知。最终评审引起代码变更时须重新运行相应测试及统一验收。

## 已明确的范围与限制

- DOCUMENT_TAX_FIRST只有配置枚举和拒绝启用保护，当前只实现GROUP_TAX_FIRST。
- 本期为后端接口；用户确认通过预览/确认契约表达，没有前端页面。没有银行收款、税务平台开票适配器，开票关联撤销不等于税务平台作废。
- 不含提现退款、跨客户转账、跨币种抵销、税务申报、自动会计凭证。
- 通知可能重复且不保证跨事件顺序；接收端必须持久化去重。默认关闭，未验证真实外部接收方。
- 新流水schemaVersion=2以十进制文本保留小数精度；旧版本JSON已丢失精度的数据不自动还原。对账检查初始化和最新快照，不宣称防篡改或全中间链验证。
- 无来源行ID表示独立的单据级来源槽，不隐式代表该单据所有具体行。
- 表结构校验只在启动时检查固定MySQL版本的业务表结构（V012 为21张，V020 为30张），不验证业务数据、权限、触发器或存储过程；不自动接管非空旧库。
- 当前为本机开发交付，未完成生产安全审查、真实身份系统集成或生产部署；Spring Boot补丁升级需在发布前单独评估。

## 最终评审状态

2026-11-03独立评审恢复后已补齐全部业务和基础设施范围，形成最终报告，无已确认Critical/Important/Minor缺陷。早先额度中断与部分报告由最终结论替代。详见[最终评审与交付结论](final-review.md)，包含评审范围、未作保证项及执行方裁定。该次评审对应 V012 核心应收基线。

## 客户现金账户 V020 验收

同一主体、客户、币种可同时持有开票与不开票现金账户。验证在 MySQL 8.0.42 完成。运行入口为 `./scripts/verify-local`。

| 业务能力 | 验收场景与证据 |
| --- | --- |
| 开户与账户查询 | 打包服务验证同用途重复开户拒绝、按客户分列账户余额、归属其他主体的查询拒绝；无依赖账户销户后可恢复原 ID。 |
| 应收归属与开票 | 打包服务核对价款和销项税两行归同一账户、调整继承原组、跨户抵销拒绝、不开票账户无可开额度。应收详情逐行返回归属。 |
| 收款充值、两种核销入口及撤销 | 打包服务逐笔核对原收款和账户 FIFO 共用资金，跨批次核销按出资收款返回原核销单 ID，反核销恢复批次。 |
| 冻结与止付 | `startup_holds.py` 验证冻结分摊、部分释放及重复请求。打包服务验证整户止付期间仍能入账，新的支出被拒。 |
| 并发与信用分户 | `startup_cash_races.py` 并发确认指定收款与账户核销，并发冻结与核销，均只有一方能占用同一笔资金。信用用例覆盖同户转入抵扣、跨户拒绝及撤销；CreditReversalLockTest 验证撤销按现金、信用账户依次加锁。 |
| 线下提现与充值冲正 | `startup_withdrawal.py` 验证独立复核、认领前撤销、结果未知保留冻结、确认失败释放、成功按原批次扣款和重复回单拒绝。打包服务验证未使用充值可整批冲正，已支出批次受保护。 |
| 现金对账、期初与存量切换 | CashCutoverRealTest 验证映射缺项、跨户、不匹配指纹、原收款及信用反核销。 `startup_cash_cutover.py` 使用打包命令验证有冻结差异时拒绝、预演、执行和重放，再经 HTTP 核对期初流水及持续分户。 |
| 结构与交付物 | 正式迁移 V001–V020；[数据字典](ar-data-dictionary.md)覆盖30张表、428个字段，[ER 图册](handbook/05-entity-relationships.md)列出中文字段含义及42条物理外键。 |

2026-11-08 完整执行通过。Python 配置检查 5 项，默认 Java 测试实际执行 46 项，业务实库实际执行 202 项，独立迁移与结构检查 13 项，打包服务 15 组。专属临时库已清理，外部通知派发关闭。运行摘要保存在本机 work/cash-v020-final.log 与 work/startup-smoke-result.json。
