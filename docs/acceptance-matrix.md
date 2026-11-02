# 应收后端需求验收矩阵

核对日期：2026-11-03；实现基线：2ed3dd8。此表记录实现及已有验证证据，最终独立分支评审仍在进行，不能据此宣称生产发布就绪。

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
- 表结构校验只在启动时检查固定MySQL版本的21张业务表结构，不验证业务数据、权限、触发器或存储过程；不自动接管非空旧库。
- 当前为本机开发交付，未完成生产安全审查、真实身份系统集成或生产部署；Spring Boot补丁升级需在发布前单独评估。

## 最终评审状态

2026-11-03已派发新的只读全分支评审（实现截至2ed3dd8）。评审反馈已读鉴权、通知派发/租约/重试、开票额度及作废锁定路径，尚未确认问题；随后因评审工具调用额度限制终止，没有形成完整报告。该反馈不等于评审通过。最终评审及整体完成门禁保持未勾选，需要恢复完整独立评审后再作结论。
