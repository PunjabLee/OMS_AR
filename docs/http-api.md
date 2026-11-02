# HTTP 接口与鉴权

当前已实现的业务接口如下；本服务为后端，不包含操作前端。

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

应收详情逐行返回`exchangeBasis`计算快照与`version`，来源记录补充`originalLineId`。依据与应收、来源和初始化流水同事务保存，后续核销不重算依据。其他业务类型禁止夹带汇率依据。汇率差异开票资格由下述显式配置控制，默认关闭；保存依据不代表可开票。

## 开票额度查询（只读）

POST `/api/finance/invoice-links/eligibility`：`legalEntityId`、`customerId`、`currency`、不重复的 `groupIds`（最多200），要求 `ar:read`。返回共享额度分组 `buckets`、逐行归属/剩余额 `lines` 和快照指纹，不登记或占用开票金额。

每组按“开票业务组＋amountType＋taxRate”计算：`remainingNetAmount = receivableNetAmount - invoicedNetAmount`，金额均保留原正负方向；`blueAvailableAmount=max(remainingNetAmount,0)`。关联负数归adjustment_link.related_group_id，其他明细归物理业务组。实际收款、抵销去向、转入余额及未核销余额不参与此公式。NULL税率与0税率不同。

逐行`remainingAmount`是该行自身剩余额，不能把多行可用额相加后越过共享分组额度；后续登记必须同时校验行额度和分组额度。负的分组净余额不代表可直接红字开票，红字仍需原发票及原分配的剩余额约束。

`invoiceSupported=false`说明该行目前不能贡献开票额度，原因见`blockedReason`。汇率差异默认`EXCHANGE_DIFFERENCE_DISABLED`；仅显式设置 `AR_INVOICE_EXCHANGE_DIFFERENCE_ENABLED=true` 才开启，并仍要求存在汇率依据，否则`EXCHANGE_BASIS_MISSING`。该配置不改变应收余额和核销资格。

此接口只提供查询；登记接口在事务内重新校验，不能以查询通过替代写入前校验。

指定originalLineId的负数调整必须保留原行税率（数值相等即可，NULL与零不同）。税率变化应以原税率负数冲回，再由新税率应收承载。历史关联若价税类型或税率不一致，开票额度查询拒绝计算并要求先更正调整，不静默改写税率或返回未减少的旧税率额度。


## 开票关联预览、登记及撤销

本服务保存外部发票与应收的关联，不调用税务平台。关联撤销不等于实际发票作废。

POST `/api/finance/invoice-links/preview`，要求 `ar:invoice:write`：`legalEntityId`、`customerId`、`currency`、`kind`（BLUE/RED）、`originalInvoiceId`（RED必填，BLUE不传）、`totalAmount`、`lines`。每行传 `invoiceLineId`、`receivableLineId`、`amount`、`taxRate`（与应收一致，可空）、`originalAllocationId`（RED必填，BLUE不传）。BLUE金额为正，RED金额为负，合计必须等于分配之和。最多500条分配、涉及最多200个开票业务组。返回逐行校验分配及fingerprint，不预占额度。

BLUE同时受行自身余额和原业务组/价税类型/税率的共享额度限制。RED同时受负数应收自身未开票额和原蓝字分配剩余可红金额限制；原蓝字必须有效且范围相同。关联组/原行负数须引用该组/指定原行的蓝字分配；独立负数可引用同范围、同类型、同税率的原蓝字分配。一条外部红字行可分配到多条原分配，必须分别提供originalAllocationId。

POST `/api/finance/invoice-links/register`，要求 `ar:invoice:write`：`requestKey`、`sourceSystem`、`sourceId`、`invoiceNo`、`invoiceDate`、原`preview`对象、`expectedFingerprint`、`reason`。返回关联ID；相同请求重放返回原ID，不同请求重复外部来源拒绝。来源在主体内唯一，撤销后也不复用。预览变化返回409并要求重新预览。

登记采用READ_COMMITTED事务：RED先锁原BLUE，再按ID锁开票业务组，取得锁后重读额度，以版本CAS更新已开票金额。单头、分配、行已开票金额、完整前后流水和幂等结果原子提交。收款、核销、转账户及未核销余额保持原值。税率不一致必须先生成旧税率负数调整和新税率应收，不直接覆盖原行。

GET `/api/finance/invoice-links/detail?id=...&legalEntityId=...`，要求 `ar:read`，返回header与allocations；有效BLUE逐分配返回redAvailableAmount，RED或已撤销BLUE该值为null。

POST `/api/finance/invoice-links/reverse`，要求 `ar:reverse`：`requestKey`、`legalEntityId`、`invoiceId`、`reason`。按持久化分配精确恢复已开票金额、保留历史并新增逆向事件；其他发票分配不变。有有效RED时BLUE拒绝撤销，须先撤销RED。撤销RED可能使原组净开票额度再次为负，此时新BLUE仍受当前共享额度约束；撤销不自动生成应收调整或修改外部发票。


## 应收作废

POST `/api/finance/receivables/void`：`requestKey`、`legalEntityId`、`documentId`、`reason`，要求`ar:void`。有权限财务直接执行，无审批；原因必填。整单原子作废。

POST `/api/finance/receivables/void-upstream`：`requestKey`、`legalEntityId`、`sourceSystem`、`sources`、`reason`，要求精确权限`ar:void:upstream:<sourceSystem>`。sources最多200个不重复对象，每个包含sourceType/sourceId/可选sourceLineId；取消所选来源槽对应的全部应收行，可跨应收单。NULL行只匹配NULL，空字符串和具体行各自独立，取消所有具体行须显式列出。取消先于生成到达也会保存来源拦截。

响应`eventId/documentIds/lineIds`：事件ID及本次发生变化的单据、作废行。相同请求重放返回原结果；新请求重复作废不再次改变余额。部分来源取消后其他行仍有效，全部行VOID后单头才VOID；部分作废原因在对应事件中，整单作废原因/操作人/时间及version由详情返回。

有效核销、余额转入、开票关联或非零占用均阻止作废；须先通过原业务撤销。外部有效调整指定原行时先取消调整；不能扩大原组/价税类型/税率的共享开票缺口。原额、税率、来源及历史分配保留，只将作废行open归零。来源blocked阻止新事件重新生成，不提供隐式恢复或自动解除入口。

事务锁序为来源→单头→有序物理及开票业务组→行，READ_COMMITTED重读依赖；状态、来源拦截、完整前后快照和幂等结果原子提交。此动作不自动撤销收款或外部税务发票。外部通知投递仍待后续任务实现。
