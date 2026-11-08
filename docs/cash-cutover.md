# 存量客户现金分户切换（V020）

一次切换覆盖一个主体、客户和结算币种。先在应用开立 INVOICE 和 NO_INVOICE 两个可用账户。财务逐一核准该范围内的所有业务组和实际收款归属，包括已结清、已撤销核销所涉及的记录。税额、已开票行和历史发票关联必须归开票账户。原行调整、历史收款核销、正负抵销和信用抵扣须能在同一账户内复原。

## 清单与试算

以真实数据库 ID 替换示例中的值。groups 的键为业务组 ID，receipts 的键为实际收款 ID，值均为现金账户 ID。空集合写成 `{}`。

```json
{"legalEntityId":1,"customerId":7,"currency":"CNY","actor":"finance-operator","reason":"财务核准本期分户","groups":{"101":501,"102":502},"receipts":{"201":501,"202":502}}
```

1. 完成 V020 结构升级，构建可执行 JAR。设置目标环境的 DB_URL、DB_USERNAME、DB_PASSWORD 及 AR_JDK_HOME，使用受控数据库身份。
2. 执行 `./scripts/cash-cutover preview manifest.json`。核对 groupCount、receiptCount、逐户现金期初 cashBook 及非现金余额 creditBalance，保留 fingerprint。发现缺项、跨户或对账差异时修正清单或源业务后重新试算。
3. 暂停并排空该范围的全部写入，包括上游任务与财务操作。取得备份，在停写窗口再次试算、核对后执行 `./scripts/cash-cutover apply manifest.json <fingerprint> --writes-drained`。指纹变化会中止。映射、资金批次、信用归属、期初流水、待投递通知和切换标记在一个事务中提交。
4. 以返回的 eventId 核对账户流水，复核各账户余额、批次、原单关联及现金专项对账。确认所有实例已部署 V020 代码，再恢复该范围写入。已提交的切换标记自动启用并持续强制分户。AR_CASH_BINDING_SCOPES 可单独为全新的空范围提前启用分户。保留原核销单 ID 供反核销。
5. 提交结果不明时用同一清单、同一指纹重试，核对保存的期初报告。已成功的范围只接受原清单重放。事务失败时先确认切换标记及写入暂停状态，再修复并重新试算。切换后已发生业务时，回退需结合备份、切换标记及后续流水单独制定恢复方案。
