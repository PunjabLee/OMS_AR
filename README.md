# OMS 应收服务

Java 21 / Spring Boot 3.5.0 / MySQL 8.0.42 后端。应收按价款、销项税分行保存余额，支持来源追溯、收款、按业务组先税后价核销、负数抵销、客户余额转入及使用、蓝红字开票关联与撤销、财务/上游作废、生命周期与对账查询，以及通知队列、自动投递和人工重试。当前没有前端、银行收款或税务平台开票适配器。

## 本机运行

需要 Docker、Maven、Python 3 和 JDK 21。当前开发机使用IDEA安装的Microsoft OpenJDK21；其他机器通过AR_JDK_HOME指定JDK目录。

1. 按.env.example准备项目根.env，设置独立数据库密码和私密随机AR_API_TOKEN。已有.env请保留，不要覆盖。该文件不提交Git。可用Python的secrets.token_urlsafe(36)生成令牌，将结果只保存到本机环境或.env；示例不包含默认令牌。
2. 在项目根执行docker compose up -d mysql，启动本项目MySQL8.0.42，端口默认为127.0.0.1:13306。不会使用或修改其他Docker数据库。
3. 执行./scripts/mvn-java21 -B -Dmaven.repo.local=work/m2 package。
4. 执行./scripts/run-local --check检查启动配置，然后执行./scripts/run-local。服务在前台运行，默认127.0.0.1:8080，Ctrl-C正常停止。

run-local将.env当作文本解析，不执行其中的命令、不展开变量；支持KEY=VALUE及成对单/双引号，值中的#和=保留为字面内容。进程环境变量优先于.env。DB_URL/DB_USERNAME/DB_PASSWORD可显式覆盖；未设置时使用本机oms_ar、oms_ar用户及MYSQL_PASSWORD。API令牌必填且至少32字符。密码和令牌仅通过子进程环境传入，不进入Java命令参数；数据库root密码不传给应用。

启动时执行正式Flyway迁移，并校验21张业务表的结构摘要。无历史非空库、迁移文件变更或结构不一致会阻止启动，不自动清库或接管旧库。生产迁移资源位于oms-ar-server/src/main/resources/db/migration。详见[数据库迁移说明](docs/database-migrations.md)。

## 调用与权限

所有HTTP接口都需要Authorization: Bearer令牌。默认权限只有ar:create、ar:read，其他操作按角色增加AR_PERMISSIONS，完整接口、请求示例和权限见[HTTP说明](docs/http-api.md)。身份与主体权限由服务端配置，不能通过请求自报操作人。

通知外发默认关闭。人工重试不会绕过派发开关；启用外发必须配置固定可信接收端及两个派发开关，接收方按幂等键去重。没有真实接收端时，业务事件仍会进入队列。

## 完整本地验证

在项目Docker数据库运行且.env配置完成后执行：

```sh
./scripts/verify-local
```

依次运行启动脚本单元测试、Java默认测试及打包、业务实库测试、正式迁移/结构漂移测试、打包应用启动和HTTP验收。任一步失败即停止，不将后续成功覆盖前一步错误。

oms_ar_test用于业务夹具，首次自动创建，后续不清空；这些用例会保留测试数据。迁移和启动验收另建随机命名的专属临时库，结束后只清理本次创建的库和进程，不清理应用数据库或Docker卷。启动验收仅监听随机本机端口、临时令牌不输出、不连接外部通知接收端；其结果记录在忽略目录work/startup-smoke-result.json。

也可单独执行scripts/test-mysql-all、scripts/test-mysql-migration和scripts/test-application-startup。默认mvn test不连接数据库，实际写库测试均有显式门禁。测试专用关闭迁移/结构校验配置不会打入应用JAR。

## 业务与开发手册

[完整手册目录](docs/handbook/README.md)包含业务规则与流程、代码模块与实际数据模型、关键算法与事务并发、接口示例和运行排障，并提供Mermaid流程、时序、状态和ER图。

## 设计与状态

- [设计基线](docs/superpowers/specs/2026-10-31-ar-project-design.md)
- [数据模型](docs/ar-data-model.md)
- [开发进度与验证记录](docs/progress.md)

本期本机后端交付已完成，见[需求验收矩阵](docs/acceptance-matrix.md)和[独立评审结论](docs/final-review.md)；预留的DOCUMENT_TAX_FIRST策略未启用，实际核销使用GROUP_TAX_FIRST。外部通知采用可重试、可能重复的投递机制，不宣称跨系统严格一次处理。
