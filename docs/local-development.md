# 本地开发环境

使用 Microsoft OpenJDK 21.0.13.0.2，已验证项目脚本可使 Maven 3.9.16 使用该 JDK。系统默认 Java 不变。

在项目根运行 `./scripts/mvn-java21 -version` 验证。其他机器通过 `AR_JDK_HOME` 指定 JDK 21 路径。

数据库由 `compose.yaml` 管理，镜像固定 mysql:8.0.42，连接地址 127.0.0.1:13306，库名和用户名均为 oms_ar。密码保存在项目根 `.env`，该文件权限为600并已列入.gitignore，禁止提交。`.env.example`仅为模板。

启动：`docker compose up -d mysql`。查看状态：`docker compose ps`。停止：`docker compose stop mysql`。数据保存在本项目的命名卷中，常规停止不会清除。不要用删除卷的命令作为普通停止方式。

应用配置对应 DB_URL=jdbc:mysql://127.0.0.1:13306/oms_ar、DB_USERNAME=oms_ar，DB_PASSWORD 使用.env内MYSQL_PASSWORD值；应用尚未实现自动加载.env，运行时需显式注入环境变量。无Nacos配置。

正式迁移已接入，启动时自动执行classpath:db/migration；无历史非空库不会自动接管。迁移规则和专属临时库测试见[数据库版本迁移](database-migrations.md)。应用启动脚本及完整启动验收仍在后续任务完成。
