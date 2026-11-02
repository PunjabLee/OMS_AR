# 应收系统基础阶段 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** 建立可测试的新工程基础、价税分配算法及数据库模型验证入口。

**Architecture:** 父模块 oms-ar + 服务模块 oms-ar-server；分层优先，业务事务与流水原子提交。

**Tech Stack:** Java 21、Spring Boot 3.5、MyBatis-Plus、MySQL 8.0.42、JUnit 5/Mockito/AssertJ；无 Nacos。

**Spec:** ../specs/2026-10-31-ar-project-design.md；../../ar-data-model.md。

## Global Constraints

遵循根AGENTS.md。主键自增、不使用雪花；审计由Service填写；POST对象入参；不允许无鉴权的写入口。不得引入额外测试依赖或接入业务数据库运行默认测试。版本基线不降为Java17。

## Review Focus

1. 同幂等键不同内容不得静默复用（后续应收创建阶段）。
2. 无来源行ID也须去重（任务3）。
3. 无关联跨类型抵销不改税额属性（任务2）。
4. 正负净额零不等于结清（任务2）。
5. 默认测试不得访问Nacos或真实数据库（任务1）。

## Task 1：工程及配置边界

Files：pom.xml、oms-ar-server/pom.xml、oms-ar-server/src/main/resources/application.yml、oms-ar-server/src/main/java/com/punjab/oms/ar/ArApplication.java、oms-ar-server/src/test/java/com/punjab/oms/ar/ConfigurationBoundaryTest.java。

接口：本阶段建立模块与测试运行入口，不开放业务HTTP接口。

- [x] 确保Java21可用；选择并锁定Spring Boot 3.5补丁版本及兼容MyBatis-Plus版本。
- [x] 创建父子模块和配置边界测试：配置不得包含nacos导入、真实主机、密码字面值。
- [x] 执行 mvn -pl oms-ar-server -am test，记录初始失败。
- [x] 实现环境变量配置，默认单元测试不启动外部连接。
- [x] 重跑同命令，全部通过后提交阶段变更。

## Task 2：纯分配算法及金额规则

Files（均位于服务模块src/main/java/com/punjab/oms/ar下）：enums/AmountType.java、enums/AllocationStrategy.java、domain/dto/finance/AllocationPreviewRequest.java、domain/vo/finance/AllocationPreviewResponse.java、service/finance/AllocationService.java、service/finance/impl/AllocationServiceImpl.java。测试位于对应src/test/java下的service/finance/AllocationServiceImplTest.java。

接口：AllocationPreviewResponse preview(AllocationPreviewRequest request)。请求包含按用户确认顺序排列的组和行、每行类型与剩余金额、可用金额、策略；响应包含逐行分配与未分配金额。该服务不持久化。

- [x] 写测试：A价100税13、B价200税26、金额120，分配依次13/100/7/0；金额10仅抵税10；金额超过应收返回未分配差额。
- [x] 写测试：价100、税5、独立负税20抵销后税0价85，不改变明细类型；正负100未抵销时不得结清；预留策略未实现时拒绝。
- [x] 运行 mvn -pl oms-ar-server -Dtest=AllocationServiceImplTest test，确认失败。
- [x] 使用BigDecimal实现纯算法，明确精度校验，禁止输入对象原地改写。
- [x] 定向测试和模块测试均通过后提交。

## Task 3：数据库迁移和模型约束

Files：scratch/dbddl/V001__ar_schema.sql、docs/ar-data-model.md、oms-ar-server/src/test/java/com/punjab/oms/ar/dao/finance/ArSchemaRealTest.java。

接口：提供表结构与约束，尚不对外暴露可写业务接口。

- [x] 按数据模型写MySQL实库测试，使用独立测试库和ar.schema.real门禁；建表动作另需ar.schema.real.commit开关。
- [x] 测试无来源行ID的重复来源事件输出仍被拒绝；重复客户账户被拒绝；非法符号/越零金额被拒绝。
- [x] 编写MySQL8.0.42 DDL，覆盖索引、外键、CHECK和自增主键。
- [x] 默认mvn test验证不连接外部环境；隔离库可用后使用明确开关执行ArSchemaRealTest，记录结果。
- [x] 未完成实库验证时明确标记SQL未验证，不将其算作完成验收。

## 后续阶段

基础阶段后依次细化并执行：应收生成及来源幂等；核销确认与反核销；客户余额批次；开票与调整链；作废权限；生命周期及对账。每阶段使用真实已有文件与服务契约制定细化任务，不能用本基础计划宣称全部业务已实现。

## 验收状态（2026-11-03核对）

基础阶段已完成；历史红灯和绿灯记录见docs/progress.md任务1、2a、3a、4b。任务2的正负净额零状态测试按已记录裁定迁至ReceivableCreateRealTest.netZeroNotSettled；重复账户测试随客户余额阶段实现。DDL按业务阶段拆分，正式迁移现为V001—V012。

Java21、MySQL8.0.42已验证，分支feature/ar-foundation；配置测试、11项纯分配测试及真实数据库约束测试已通过。2026-11-03统一验证入口scripts/verify-local全部通过，记录work/local-verification-final.log。该基础阶段完成不代表最终全分支验收完成，最终状态见2026-11-02-ar-migration-release.md。
