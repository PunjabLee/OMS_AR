# 数据库版本迁移

正式SQL位于oms-ar-server/src/main/resources/db/migration，当前V001至V012。应用使用Spring Boot管理的Flyway11.7.2及flyway-mysql模块，启动时迁移并校验历史文件。产物已包含全部12个版本。接入方式参照[Spring Boot 3.5官方初始化说明](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html)。

## 版本规则

- 已执行的版本文件不得修改、删除或更名；后续变更新增更高版本SQL。
- 正式CREATE TABLE不含IF NOT EXISTS，不能静默跳过已有但不一致的表。
- baseline-on-migrate=false，不自动接管无历史的非空库；clean-disabled=true，禁止通过应用清库；validate-on-migrate=true，校验失败阻止启动；不自动执行repair。
- 数据库连接仍由DB_URL/DB_USERNAME/DB_PASSWORD提供，无默认密码，无Nacos。通知派发仍默认关闭。
- MySQL DDL存在隐式提交。失败迁移可能已留下部分表结构，必须先核查数据库、备份和实际失败语句，再制定修复方案，不能通过自动重跑或直接修改历史校验和掩盖问题。

Flyway历史文件校验使用迁移校验和，能够发现已执行文件变化；它不等同于对现存表结构进行逐列检查，不能据此宣称手工ALTER已被检测。该边界参照[Flyway Validate说明](https://documentation.red-gate.com/flyway/reference/commands/validate)。结构完整性与启动验收在下一任务完成。

## 开发与测试库

生产运行只读取classpath迁移资源，不执行scratch/dbddl。scratch内的IF NOT EXISTS SQL仅保留给已有隔离业务测试夹具；MigrationResourcesTest逐文件校验夹具与正式版本仅存在幂等建表关键字差异，避免两个来源结构分叉。历史业务实库测试的pre配置关闭Flyway，因此不会自动接管现有oms_ar_test。正常应用不使用该测试资源配置。

执行./scripts/test-mysql-migration验证正式迁移。脚本在项目Docker MySQL中创建带随机前缀oms_ar_migration_的5个专属临时库，并仅授予现有oms_ar用户这些临时库的权限；结束时撤销权限并删除本次创建的库。不会清理oms_ar、oms_ar_test或其他服务数据库。密码不进入日志。测试覆盖空库初始化、重复执行、V6至V12升级保留数据、修改历史文件拒绝继续、非空无历史库拒绝接管，以及部分DDL失败后的停止行为。

./scripts/test-mysql-all运行原有业务实库回归，迁移用例在其中保持门禁关闭，需单独运行上面的迁移脚本；默认mvn test不连接数据库。部署前应同时完成默认测试、业务实库、迁移测试和应用启动验收。
