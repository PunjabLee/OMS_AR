# 现金账户与应收归属联调

V013 引入现金账户，V014 引入业务组归属，V015 将实际收款唯一绑定至充值批次。当前联调覆盖开户、充值、按收款单或按账户核销、原核销单撤销、负数抵销和开票资格。V016–V019 已加入金额冻结、线下提现复核、非现金信用余额分户和充值冲正。历史范围按[存量分户切换步骤](cash-cutover.md)导入。

## 账户与创建

POST /api/finance/cash-accounts/open 接受 requestKey、legalEntityId、customerId、currency、invoiceMode、reason。权限为 ar:cash:open，invoiceMode 取 INVOICE 或 NO_INVOICE。GET /api/finance/cash-accounts/detail 按 id、legalEntityId 查询，要求 ar:cash:read，返回账面、冻结和可用额。

在隔离联调环境将 AR_CASH_BINDING_SCOPES 配置为 主体ID/客户ID/币种，可用逗号连接多个范围，例如 1/7/CNY。默认范围为空。对已有业务数据的范围，应先完成切换。已提交的切换标记在所有实例持续启用分户，删除配置也不能绕过。启用范围内已存在的未绑定组追加或调整时会返回冲突。

创建应收时，新业务组至少一行指定 invoiceMode。同一 groupKey 的其他行可以省略并继承。已有组和指定原行的调整可以全部省略，若显式指定则须与原归属一致。归属行与创建快照同事务写入 oms_ar_cash_group_binding，可用应收行 group_id 追溯。物理组和原开票额度组仍分别存储。

例如价款行 100 元与税额行 13 元使用相同 groupKey，税额行选择 INVOICE，两行均归开票账户。NO_INVOICE 组允许价款行，拒绝销项税行。开票额度查询会将不开票账户的明细标为 NON_INVOICE_ACCOUNT，不贡献共享额度。正负抵销只接受同一账户的负数来源和目标组。

## 验证

scripts/test-mysql-migration 核对归属唯一性与跨客户外键。scripts/test-application-startup 在临时数据库验收双账户开户、价税共同归户、后补税与原行调整继承、禁止跨户抵销、不开票账户禁止预览蓝字及同户抵销确认。

## 收款充值与两种核销入口

分户范围内调用 `POST /api/finance/receipts/create` 时增加 `cashAccountId`。一次成功登记同时保存实际收款、唯一充值批次、现金账户余额变化和业务流水。账户必须属于同客户、主体和币种。仍以原收款来源唯一键防止换账户重复充值。

指定收款使用原 settlements/preview 和 settlements/confirm。新账户入口位于 `/api/finance/cash-accounts/settlements/preview` 与 `/api/finance/cash-accounts/settlements/confirm`。预览传 legalEntityId、accountId、amount、groupIds 和 GROUP_TAX_FIRST，权限为 ar:settle。返回目标应收分配、各批出资的 lotId、receiptId、amount，以及 usedAmount、unallocatedAmount 和 fingerprint。确认传原 preview、expectedFingerprint、requestKey 和 reason。

账户核销按充值批次 ID 升序供资，每张实际出资收款形成一张 RECEIPT 核销单，在同一事务内全部提交。返回 eventId 与 settlementIds。相同请求重放返回相同列表。按 settlementIds 查询核销详情或调用原 settlements/reverse，可按原始收款和应收分配恢复金额；逐单反核销为独立事务。

例：AA 收款 100 和 80，账户入口先核销 60 后，账户可用 120，第一张收款单最多直核 40。再从账户核销 55 时，两批分别供资 40、15，账户剩 65。上述场景及按原批次恢复已纳入打包应用测试。

## 冻结、止付与账户生命周期

金额冻结调用 `POST /api/finance/cash-accounts/freeze`，传入 legalEntityId、accountId、amount、requestKey 和 reason，要求 ar:cash:freeze。系统按原充值批次 FIFO 冻结，返回 holdId。同一账户可持有多张冻结单。`unfreeze` 另传 holdId，可以按原分摊部分释放。普通解冻仅限人工冻结单。`GET /api/finance/cash-accounts/holds/detail` 用主体、账户和冻结单 ID 查询各批次原冻结、累计释放及累计扣款。

整户 stop、resume、close、reopen 均接收主体、账户、expectedVersion、requestKey 和 reason，分别使用对应的 ar:cash 权限。止付期间可入账和解除占用。销户要求账面及冻结额归零、信用余额归零、无未结应收和在途提现。恢复沿用同一个账户 ID。

## 提现与充值冲正

在 `/api/finance/withdrawals` 下，通过 payout-accounts/bind、payout-accounts/disable 维护受托付款档案。bind 保存受托受益人引用及尾号，detail 返回脱敏信息。apply 预留资金，approve 或 reject 要求独立财务身份，申请人可在付款认领前 cancel。claim 取得线下付款任务，result 由认领人登记 PAID、FAILED 或 UNKNOWN。成功要求唯一银行回单，失败释放须提供未付款凭据，结果未知时继续冻结。整户止付会阻止新认领，允许登记已经认领的结果。

`POST /api/finance/receipts/reverse-deposit` 要求 ar:cash:deposit:reverse、legalEntityId、receiptId、requestKey 和 reason。仅原收款批次可整批冲回。存在核销或冻结时须先解除，已有实际提现的批次拒绝冲正。原收款仍可查询，reversedAmount 显示冲回金额，availableAmount 排除冲正、已核销、已提现及冻结额。

## 查询与对账

在 `/api/finance/cash-accounts` 下 POST lots/page 或 ledger/page，以 legalEntityId、accountId、page、size 查询每批资金来源和账户变动。POST reconciliation/page 则按 legalEntityId、customerId、currency、page、size 返回差异。三者均要求 ar:cash:read。专项对账覆盖账户账面与批次、冻结投影与原分摊、提现扣款与回单快照、收款占用与有效核销分配。

V018 为非现金信用账户增加 cashAccountId。分户范围的负数转入、信用抵扣及其逆向动作均按原业务归属，现金余额与信用余额独立查询。

## 客户账户与应收归属查询

使用 `GET /api/finance/cash-accounts/by-customer?legalEntityId=1&customerId=7&currency=CNY` 查询同一结算范围的全部账户（含已关闭账户）。每项返回账户 ID、开票属性、状态、账面余额、冻结额、可用余额和版本。发起冻结、提现或账户核销时，使用选中项的账户 ID。

`GET /api/finance/receivables/detail` 的每条应收明细返回 `cashAccountId` 和 `invoiceMode`。历史未归户组的这两个字段为空。已归户的价款和税额行显示同一账户。
