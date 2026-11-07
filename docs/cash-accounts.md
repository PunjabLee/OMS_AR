# 现金账户与应收归属联调

V013 引入现金账户，V014 引入业务组归属。当前阶段适用于开户、应收归属、负数抵销和开票资格联调。实际收款及信用余额抵扣路径仍使用旧模型，生产资金分户切换须等待充值批次、核销及存量映射完成。

## 账户与创建

POST /api/finance/cash-accounts/open 接受 requestKey、legalEntityId、customerId、currency、invoiceMode、reason。权限为 ar:cash:open，invoiceMode 取 INVOICE 或 NO_INVOICE。GET /api/finance/cash-accounts/detail 按 id、legalEntityId 查询，要求 ar:cash:read，返回账面、冻结和可用额。

在隔离联调环境将 AR_CASH_BINDING_SCOPES 配置为 主体ID/客户ID/币种，可用逗号连接多个范围，例如 1/7/CNY。默认范围为空。此配置适用于本阶段验证，应将范围保持稳定；它不执行数据迁移。启用范围内已存在的未绑定组追加或调整时会返回冲突。

创建应收时，新业务组至少一行指定 invoiceMode。同一 groupKey 的其他行可以省略并继承。已有组和指定原行的调整可以全部省略，若显式指定则须与原归属一致。归属行与创建快照同事务写入 oms_ar_cash_group_binding，可用应收行 group_id 追溯。物理组和原开票额度组仍分别存储。

例如价款行 100 元与税额行 13 元使用相同 groupKey，税额行选择 INVOICE，两行均归开票账户。NO_INVOICE 组允许价款行，拒绝销项税行。开票额度查询会将不开票账户的明细标为 NON_INVOICE_ACCOUNT，不贡献共享额度。正负抵销只接受同一账户的负数来源和目标组。

## 验证

scripts/test-mysql-migration 核对归属唯一性与跨客户外键。scripts/test-application-startup 在临时数据库验收双账户开户、价税共同归户、后补税与原行调整继承、禁止跨户抵销、不开票账户禁止预览蓝字及同户抵销确认。
