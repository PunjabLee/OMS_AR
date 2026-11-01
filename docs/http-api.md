# HTTP 接口与鉴权

当前实现：创建、详情、分页。其余业务仍在建设，不能调用尚未实现的核销或作废动作。

- POST `/api/finance/receivables/create`：ReceivableCreateRequest JSON，成功data为单据ID。
- GET `/api/finance/receivables/detail?id=...&legalEntityId=...`：包含逐行余额、来源、税率、分类汇总和结算状态。
- POST `/api/finance/receivables/page`：`{"legalEntityId":1,"customerId":1,"page":1,"size":20}`，客户可省略，size不超过200。

统一响应 `{code,data,msg}`，成功code=0；未鉴权HTTP401，主体/动作越权403，格式校验400，业务冲突409，内部错误500且不暴露SQL。

## 本地服务端配置

应用读取环境变量：DB_URL、DB_USERNAME、DB_PASSWORD、AR_API_TOKEN（至少32字符随机值）、AR_API_USER（默认local-finance）、AR_LEGAL_ENTITIES（默认1）、AR_PERMISSIONS（默认ar:create,ar:read）。

Authorization请求头格式：`Bearer <token>`。身份与权限来自服务端配置，不接受X-User等自报身份。AR_API_TOKEN为空时所有HTTP访问默认拒绝。生产集成需要由部署环境注入不同调用方的独立凭据，在ar.auth.clients配置多项；通过TLS或可信网关传输，应用默认仅监听127.0.0.1。本阶段提供服务端凭据鉴权，并非完整用户登录/账号管理系统。

配置对象不打印token，凭据不能入Git。不会把开发凭据或数据库密码写入文档。

## OpenAPI

采用springdoc 2.8.9，兼容矩阵：https://springdoc.org/v2/faq.html 。默认关闭文档端点及Swagger UI；设置AR_API_DOCS_ENABLED=true后可携带相同访问凭据请求 `/v3/api-docs`，UI仍关闭。

## 验证

`./scripts/test-mysql-http`在随机本机端口启动服务并使用oms_ar_test验证无凭据拒绝、创建、重复请求和详情；默认测试不启动真实HTTP/数据库，需显式测试脚本。

## 收款

POST `/api/finance/receipts/create`登记收款，要求 `ar:receipt:create`；GET `/api/finance/receipts/detail?id=...&legalEntityId=...`查询原额、已分配、可用余额及版本，要求 `ar:read`。登记请求包含requestKey/customerId/legalEntityId/currency/amount/receivedDate/sourceSystem/sourceId/sourceNo，来源在主体内唯一。登记行为仅代表授权收款系统确认的实际收款，不调用银行，不伪造银行确认。

## 收款核销

POST `/api/finance/settlements/preview`：`legalEntityId`、`receiptId`、`amount`、有序`groupIds`、`strategy=GROUP_TAX_FIRST`。明确选择组内全部有效正数应收（可能跨单），响应列出实际分配行、版本、实际使用金额、未分配金额和fingerprint。预览不占款。

POST `/api/finance/settlements/confirm`：`requestKey`、原`preview`请求对象、`expectedFingerprint`、`reason`。预览与确认要求`ar:settle`；补税、余额变化或版本变化会拒绝旧预览，必须重新确认。超出目标应收的金额保留为收款可用余额，不转信用账户。

POST `/api/finance/settlements/reverse`：`requestKey`、`legalEntityId`、`settlementId`、`reason`，要求`ar:reverse`。整单按原分配恢复，保留原核销单和分配关系并新增逆向事件。重复反核销不会重复恢复余额。

反核销入口支持收款核销与正负应收抵销；也支持信用余额抵扣的原分配恢复。

## 正负应收抵销

创建负数应收行可传 `originalLineId`，必须关联同主体、客户、币种、金额类型的有效正数行。指定既有 `groupId` 或原行时生成不可变调整关联；指定两者时必须为同组。关联单独存储，即使没有来源单据也不会丢失。原行已结清仍可关联；正数应收不能指定原行。

POST `/api/finance/offsets/preview`：`legalEntityId`、`negativeLineId`、正数 `amount`、有序 `groupIds`、`continueOtherTargets`、`strategy=GROUP_TAX_FIRST`。要求 `ar:settle`。有关联原行默认仅抵原行；仅有关联组则抵组内同类型。用户确认 `continueOtherTargets=true` 后，先原行、再关联组及所选组的其他同类型行。未确认时额外目标组会被拒绝。独立负数按所选组顺序、每组先税后价，可跨类型抵销。

预览返回 `linked`、`originalLineId`、负数行版本、逐行分配、`usedAmount`、请求金额未分配部分 `unallocatedAmount`、抵销后的负数余额绝对值 `remainingCredit`、`fingerprint`。未分配部分保留待核销，不自动进入客户账户。

POST `/api/finance/offsets/confirm`：`requestKey`、原 `preview` 请求、`expectedFingerprint`、`reason`。确认后生成 OFFSET 核销单；正负应收、分配记录及前后流水同事务提交。重复请求返回原核销单；余额或目标成员变化需重新预览。撤销调用通用 `/api/finance/settlements/reverse`，恢复原分配而不按当前规则重算。

## 客户信用余额转入

POST `/api/finance/customer-balances/transfer`：`requestKey`、`legalEntityId`、`negativeLineId`、正数 `amount`、`expectedLineVersion`、`reason`，要求 `ar:balance:transfer`。版本来自应收详情；可将待核销负数余额部分或全部转入，返回来源批次ID。按客户、主体和币种自动开户；同一负数可拆分多批转入。转入减少负数未核销余额并增加负数已转入金额的绝对值，已核销金额保持原值；账户余额不属于实收。

GET `/api/finance/customer-balances/lot-detail?id=批次ID&legalEntityId=主体ID` 返回来源应收行、账户ID、批次原额、已使用金额、状态及版本。GET `/api/finance/customer-balances/detail?id=账户ID&legalEntityId=主体ID` 返回账户余额和版本，两者要求 `ar:read`。

POST `/api/finance/customer-balances/reverse-transfer`：`requestKey`、`legalEntityId`、`lotId`、`reason`，要求 `ar:reverse`。整批撤销；已被使用的批次必须先撤销对应余额抵扣。恢复原负数应收，扣回账户余额，保留批次和逆向流水。账户、批次、应收与流水同事务提交，不支持退款、提现或跨客户/跨币种划转。余额抵扣入口及批次依赖见下节。

## 客户信用余额抵扣

POST `/api/finance/customer-balances/settlements/preview`：`legalEntityId`、`accountId`、正数 `amount`、有序 `groupIds`、`strategy=GROUP_TAX_FIRST`。要求 `ar:settle`。所选组必须与账户客户、主体、币种一致；按用户组顺序、组内先税后价抵扣。来源批次按转入批次ID升序FIFO使用，预览显示逐行应收分配、逐行对应批次分配、实际使用额、未分配额、账户版本及fingerprint，不占余额。

POST `/api/finance/customer-balances/settlements/confirm`：`requestKey`、原 `preview`、`expectedFingerprint`、`reason`。账户、可用批次、目标应收成员或余额变化均要求重新预览。确认创建BALANCE核销单，同时保存核销账户及每条应收分配对应的来源批次/金额。只扣实际使用额，其余留在账户；原转入应收的已转入金额保持不变，不增加实收。

通用 `/api/finance/settlements/reverse` 按原应收及原来源批次恢复，不重新执行FIFO；原单据/分配保留，新增逆向事件。某批次仍有其他核销占用时，不能撤销该批次转入。账户→批次→业务组→明细固定锁序配合版本校验，抵扣、反核销与转入撤销在同一账户上串行校验。

## 汇率差异依据

`businessType=EXCHANGE_DIFFERENCE`的每条应收必须是PRINCIPAL，且提供`exchangeBasis`：`originalCurrency`、`originalAmount`、`oldRate`、`newRate`、`rateDirection`。原币与结算币须不同，汇率为正，原币金额符合原币精度。`SETTLEMENT_PER_ORIGINAL`表示乘汇率，`ORIGINAL_PER_SETTLEMENT`表示除汇率。旧、新折算值分别用HALF_UP舍入到结算币种精度，差额为新减旧；必须等于行amount，零差额不生成应收。折算值超出金额存储范围会拒绝。

应收详情逐行返回`exchangeBasis`计算快照与`version`，来源记录补充`originalLineId`。依据与应收、来源和初始化流水同事务保存，后续核销不重算依据。其他业务类型禁止夹带汇率依据。汇率差异可开票资格仍待开票服务实现，并默认关闭；保存依据不代表可开票。
