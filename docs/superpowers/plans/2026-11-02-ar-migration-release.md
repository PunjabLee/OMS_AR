# 正式迁移与本机交付验收计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans task-by-task.

**Goal:** 以可验证的正式迁移启动完整应收服务，完成部署说明及全分支验收。
**Architecture:** Spring Boot管理的Flyway 11.7.2与MySQL模块；classpath:db/migration为正式版本源。禁止自动baseline/clean/repair。历史实库业务夹具保留独立测试库，生产不执行scratch DDL。使用新建随机命名隔离库验证迁移，结束后只删除本次创建的库。
**Tech Stack:** Java21、Spring Boot3.5.0、Flyway11.7.2、MySQL8.0.42。
**Spec:** ../specs/2026-10-31-ar-project-design.md。

## 13a 正式迁移与校验
- [x] 将V001—V012规范为生产资源，去除IF NOT EXISTS，明确版本发布后不可改写；scratch仅保留测试兼容夹具，用默认测试证明两者DDL一致。
- [x] 接入Boot管理版本的flyway-core及flyway-mysql；启动默认迁移和validate，baseline-on-migrate=false、clean-disabled=true，失败不自动repair。
- [x] 随机临时数据库实测空库、V6升级保留数据、重复执行无变化、校验和漂移拒绝、无历史非空库拒绝、失败DDL不自动继续。
- [x] 默认测试无外部连接，原有业务实库测试不自动接管无历史测试库；全量回归及独立迁移测试通过后提交。

## 13b 表结构完整性与启动验收

细分提交：13b1只读结构校验与基准；13b2启动脚本、完整启动/HTTP验收及README。
- [x] 明确Flyway校验和不等于现存表结构校验，建立必要的结构验收/漂移检测证据（包括列、索引、外键、CHECK）。
- [ ] 实测正式资源自动迁移后的应用启动和关键HTTP业务流程；确认无凭据拒绝、通知默认关闭、数据库连通性、结束进程可控。
- [ ] 提供安全加载.env的启动/验证脚本和本地部署README，不打印密码，不使用应用默认凭据。
- [ ] 测试和文档同步完成后独立提交。

## 13c 最终验收
- [ ] 从原始需求、规格与所有计划整理验收矩阵，逐条核对代码与测试证据，修正旧计划遗留未勾选状态。
- [ ] 使用新的完整分支评审，修复阻断问题并逐项单独提交/验证。
- [ ] 完整构建、默认测试、业务实库、正式迁移与启动验收全部通过；记录剩余限制，不把设计预留项写成已实现。
- [ ] 仅全部要求实证满足时完成整体目标；不推送、不合并、不发布。

Pre-flight: 13a资源为13b启动及结构验收的唯一生产DDL来源；历史夹具只在禁用Flyway的隔离业务测试中执行，需同源一致性测试防止分叉。13c核对范围不限于本迁移子计划。
