# 实施进度

目标：按六阶段完成应收后端全部设计范围，尚未完成。

已验证JDK21及本地MySQL8.0.42；初始化feature/ar-foundation。基础配置测试已编写，开始下载依赖并运行红灯测试。

裁定：新目录无已有分支或用户代码，按用户指定目录原位建立功能分支，不创建多余工作树。

后续：配置绿灯→金额算法→DDL实库约束→应收生成→核销/反核销→客户余额→开票→作废/权限/流水对账。

历史构建会话78645已结束；初始配置红灯与绿灯均已确认，无待运行构建。

已选开发起始依赖：Spring Boot 3.5.0、MyBatis-Plus 3.5.12；发布前需审核同基线补丁升级，不能将起始版本选择视为完成安全验证。

任务1完成：配置边界测试先因application.yml缺失失败，再实现环境变量配置与启动类；2026-10-31全模块test通过（1用例）。日志work/foundation-test.log与work/foundation-green.log。用户要求：每项细分任务完成独立提交Git。下一项为纯金额分配算法。


任务2a完成：纯分配算法及输入边界。请求按确认组序、组内税后价，返回分配明细和未分配资金；11个业务用例加配置用例，全模块12用例通过。先用UnsupportedOperation占位观察红灯，再实现绿灯。证据work/allocation-red.log、work/allocation-green.log。

范围：独立信用金额输入绝对值，与收款共用正数目标分配算法；信用来源合法性、指定原行同类型抵销、持久化版本检查归后续核销确认服务。不得声称纯算法已完成核销业务。

计划拆分裁定：原任务2的“正负净额零不能视为结清”属于应收余额状态计算，将作为应收生成/余额模块独立测试，不放入纯分配服务。下一任务为MySQL DDL及隔离实库验证，之后进入应收生成和核销确认。


任务3a：核心四表DDL完成：document/business_group/line/line_source。独立oms_ar_test库4项实库测试通过，默认测试门禁不访问数据库。DDL红灯是脚本缺失（4断言失败），绿灯验证MySQL CHECK/复合外键/可空来源行ID幂等。脚本scripts/test-mysql-schema读取.env密码但不打印。

裁定：DDL按业务细分提交；余额表、事件表、核销表、开票表分别随对应服务提交并验证，尚未完成全部逻辑模型。DDL测试使用直接JDBC避免把应用启动和DDL约束混为一项验证；端口暂固定本机13306，与当前Compose一致。应用生成服务必须为source_event_id生成来源系统内全局限定事件标识，不可使用仅单据行局部序号。

下一项：事件/幂等表及应收生成服务，补齐来源字段、组后补税、初始化快照和结清状态。

任务4a：生成前置身份上下文，按主体与动作双重授权，清理后不可复用；未接入HTTP认证，未开放写入口。3个新增用例先红后绿，全模块15个默认测试通过，实库4个默认门禁跳过。后续创建/核销/作废均调用此契约，不接受DTO自报操作人。

任务4b：幂等请求与生命周期事件/变化三表完成。6项实库用例通过；默认15项通过、6项实库按门禁跳过。新增用例先因表不存在失败，再验证请求键按调用方隔离、JSON初始化税率快照可回读。日志work/audit-red.log、work/audit-green.log、work/audit-default.log。

下一步不再扩展基础设施：实现ReceivableService.create事务编排、MyBatis实体/DAO/Mapper、DTO/VO/Converter，并以MySQL集成测试验证应收、来源、事件、幂等同时提交/回滚。身份上下文和三表尚未接入生成服务，不能声称创建功能已实现。

任务4c完成：ReceivableService.create统一事务、MyBatis-Plus应收聚合DAO和映射、MapStruct请求转换、参数/币种精度校验、按组键归并/按既有组ID关联、来源记录及初始化快照。生成即ACTIVE，来源冲突回滚单据/组/事件/幂等记录。幂等以主体+可信操作人+CREATE+requestKey隔离，同键异内容拒绝，数据库占位并加锁串行化相同请求。

验证：8项创建实库测试通过（含两线程同时重放、结清组追加税、失败全事务回滚、越权/精度/未知组拦截）；15项默认测试通过；14项实库测试默认门禁跳过。先执行创建服务占位红灯5项失败，后实现并扩展到8项绿灯。日志work/create-red.log、work/create-green.log、work/create-default.log。

架构裁定：ReceivableDao管理同一finance应收聚合内的document/group/line/source/operation/event/change，Service不跨业务模块DAO。审计字段显式填充，主键自增，JSON快照包含创建对象与税率。未开放HTTP写入口，鉴权适配、详情分页和生命周期查询下一任务补齐；汇率差异计算依据表、原行调整关联、核销确认、余额、开票和作废仍未实现，不算整体完成。

下阶段具体接口：ReceivableService.detail(ReceivableDetailQuery)->ReceivableResponse，查询经Query转BO进入DAO，返回VO含逐行余额/来源/税率、按金额类型汇总；结清逐行判断不能按净额零。随后接HTTP认证与只用GET/POST的控制器。测试数据仅落oms_ar_test，演示数据库oms_ar尚未部署业务表。
