# 现金账户与应收归属联调

V013 引入现金账户，V014 引入业务组归属，V015 将实际收款唯一绑定至充值批次。当前联调覆盖开户、充值、按收款单或按账户核销、原核销单撤销、负数抵销和开票资格。冻结、提现、非现金信用余额分户及历史资金切换尚待完成。

## 账户与创建

POST /api/finance/cash-accounts/open 接受 requestKey、legalEntityId、customerId、currency、invoiceMode、reason。权限为 ar:cash:open，invoiceMode 取 INVOICE 或 NO_INVOICE。GET /api/finance/cash-accounts/detail 按 id、legalEntityId 查询，要求 ar:cash:read，返回账面、冻结和可用额。

在隔离联调环境将 AR_CASH_BINDING_SCOPES 配置为 主体ID/客户ID/币种，可用逗号连接多个范围，例如 1/7/CNY。默认范围为空。此配置适用于本阶段验证，应将范围保持稳定；它不执行数据迁移。启用范围内已存在的未绑定组追加或调整时会返回冲突。

创建应收时，新业务组至少一行指定 invoiceMode。同一 groupKey 的其他行可以省略并继承。已有组和指定原行的调整可以全部省略，若显式指定则须与原归属一致。归属行与创建快照同事务写入 oms_ar_cash_group_binding，可用应收行 group_id 追溯。物理组和原开票额度组仍分别存储。

例如价款行 100 元与税额行 13 元使用相同 groupKey，税额行选择 INVOICE，两行均归开票账户。NO_INVOICE 组允许价款行，拒绝销项税行。开票额度查询会将不开票账户的明细标为 NON_INVOICE_ACCOUNT，不贡献共享额度。正负抵销只接受同一账户的负数来源和目标组。

## 验证

scripts/test-mysql-migration 核对归属唯一性与跨客户外键。scripts/test-application-startup 在临时数据库验收双账户开户、价税共同归户、后补税与原行调整继承、禁止跨户抵销、不开票账户禁止预览蓝字及同户抵销确认。

## 收款充值与两种核销入口

分户范围内调用 `POST /api/finance/receipts/create` 时增加 `cashAccountId`。一次成功登记同时保存实际收款、唯一充值批次、现金账户余额变化和业务流水。账户必须属于同客户、主体和币种。仍以原收款来源唯一键防止换账户重复充值。

指定收款使用原 settlements/preview 和 settlements/confirm。新账户入口位于 `/api/finance/cash-accounts/settlements/preview` 与 `/api/finance/cash-accounts/settlements/confirm`。预览传 legalEntityId、accountId、amount、groupIds 和 GROUP_TAX_FIRST，权限为 ar:settle。返回目标应收分配、各批出资的 lotId、receiptId、amount，以及 usedAmount、unallocatedAmount 和 fingerprint。确认传原 preview、expectedFingerprint、requestKey 和 reason。

账户核销按充值批次 ID 升序供资，每张实际出资收款形成一张 RECEIPT 核销单，在同一事务内全部提交。返回 eventId 与 settlementIds。相同请求重放返回相同列表。按 settlementIds 查询核销详情或调用原 settlements/reverse，可按原始收款和应收分配恢复金额；逐单反核销为独立事务。

例：AA 收款 100 和 80，账户入口先核销 60 后，账户可用 120，第一张收款单最多直核 40。再从账户核销 55 时，两批分别供资 40、15，账户剩 65。上述场景及按原批次恢复已纳入打包应用测试。
