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
