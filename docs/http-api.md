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

当前上述入口只支持收款核销；正负抵销与信用余额抵扣将在各自任务中接入，不提前开放预留类型。
