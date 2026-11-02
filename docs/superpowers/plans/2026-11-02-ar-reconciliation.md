# 财务对账实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 对账余额与有效业务分配，返回可定位差异，禁止自动修账。
**Architecture:** 主体/客户/币种必选，RR一致快照内SQL聚合后分页差异。每项差异展示规则、对象、存储值、重算值。不将内部对账等同外部银行或税务对账。
**Tech Stack:** Java21、MySQL8.0.42、MyBatis-Plus、JUnit5。
**Spec:** ../specs/2026-10-31-ar-project-design.md；../../ar-data-model.md。

## 11a 余额与分配守恒

- [x] 创建ReconciliationQuery/BO/Response、Service、DAO、Mapper和HTTP契约；范围必选legalEntityId/customerId/currency，ar:read，页大小最多200。
- [x] 失败测试：正常业务无差异；修改invoiced但保留表内CHECK后报告LINE_INVOICED；跨主体不能查询。
- [x] 实现LINE_SETTLED（正负有效分配）、LINE_TRANSFERRED（有效转入批次带负号）、LINE_INVOICED（有效蓝红字带符号分配）、RECEIPT_ALLOCATED、ACCOUNT_BALANCE、LOT_USED、SETTLEMENT_TOTAL、BALANCE_ALLOCATION_USAGE。
- [x] count与page共用差异SQL，固定规则/对象ID顺序，快照一致；金额以BigDecimal比较，不使用浮点。
- [x] 各规则造差异后验证，仅隔离测试库修改fixture；验证无误报/权限/分页；全部测试后单独提交。

## 11b 审计及结构完整性检查

- [ ] 检查明细open恒等式、VOID占用、初始化/末次版本快照一致性、有效分配范围/价税属性、孤立来源与关联、发票合计/红字上限。
- [ ] 明确只读报告覆盖及外部对账限制；提供可执行诊断入口，不将仅11a当成完整对账验收。

之后仍需通知outbox/重试、版本迁移、启动验收和最终全分支审查。
