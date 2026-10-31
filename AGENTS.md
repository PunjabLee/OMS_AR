# AGENTS.md

本仓库是系统自研的后端服务。本文是面向 AI 代理与新进开发者的工程指南，覆盖：项目概况、技术栈、分层架构与分层规范、测试规范、构建验证、Git 分支协作。

## 1. 项目概述

- Maven 多模块工程：parent ）→ `xxx`（主服务，全部源码）。
- 版本方案：CI-friendly `revision` + flatten-maven-plugin（`resolveCiFriendliesOnly`）。后缀由 profile 决定：dev/test → `-SNAPSHOT`，pre → `-uat`，prod → 无后缀。
- 启动类 （`@SpringBootApplication(scanBasePackages = "xxx.xxx.xxx")` + `@EnableDiscoveryClient` + `@EnableCaching`）。
- 本工程从零建设，数据库为 MySQL 8.0.42。
- 用户已取消 Nacos 地址：不接入 Nacos 配置中心，不沿用旧地址，不配置 `spring.config.import: nacos:`。
- 配置使用本地 `application.yml` 配合环境变量；数据库连接使用 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`，禁止提交密码。
- 本文后续涉及 Nacos、旧模块名及旧项目样本的描述仅为历史参考，不代表本工程已存在对应模块或需要连接旧环境。

## 2. 技术栈

- 语言与基线：Java 21、UTF-8；Spring Boot 3.5 基线。
- `spring-boot-starter-parent`，三方依赖版本:Spring Boot / Spring Cloud / MyBatis-Plus / springdoc / Hutool / MapStruct 1.6.3、Lombok 1.18.42（provided）、EasyExcel 3.3.4。

| 领域 | 组件 |
| --- | --- |
| 持久层 | MyBatis-Plus（`mybatis-plus-spring-boot3-starter`） |
| 校验 | spring-boot-starter-validation（jakarta validation） |
| 对象映射 | MapStruct + Lombok |
| 缓存 | Redis（spring-boot-starter-data-redis，见 `config/RedisConfig`） |
| 文档 | springdoc-openapi-starter-webmvc-ui + swagger-annotations |
| 工具 | Hutool、commons-lang3、EasyExcel |


## 3. 分层架构

包根 `xxx.xxx.xxx`，**分层优先**：一层目录是分层名，二层才是业务模块名。

```text
xxx.xxx.xxx
├── controller/<模块>     # REST 端点
├── service/<模块>/impl   # 业务逻辑
├── dao/<模块>/impl       # 数据访问
├── mapper/<模块>         # MyBatis-Plus Mapper 接口
├── domain
│   ├── entity/<模块>     # 持久化实体
│   ├── dto/<模块>        # 写请求（Create/UpdateRequest）
│   ├── query/<模块>      # 读请求（Page/DetailQuery）
│   ├── bo/<模块>         # Service→DAO 内部查询对象
│   ├── vo/<模块>         # 只读响应
│   └── convert/<模块>    # MapStruct Converter
├── base/                 # ApiReturn、IPageResult、CurrentUser 等
├── common/utils/         # LikeEscapeUtil 等
├── config/               # MybatisPlus、Redis、RestTemplate、线程池等配置
├── enums/                # BaseEnum 及各模块枚举
└── extapi/               # 外部系统客户端（basic/hkerp/oa）
```

- 业务模块（以 controller 层为全集）：address, base, contract, customer, finance, inventory, notice, order, ordercode, product, push, salestarget, sys, trader, trialcut, user。迁移进行中，存量模块各分层未必齐备。
- Mapper XML 位于 `src/main/resources/mapper/<模块>/`，当前仅 customer、sys 等少数模块存在（BaseMapper 已覆盖单表 CRUD）。

总体调用方向（SPEC §2）：

```text
HTTP
  -> Controller
  -> Service / ServiceImpl
  -> Converter: PageQuery -> PageQueryBO
  -> DAO / DAOImpl
  -> Mapper / Mapper XML
  -> Entity / Database

Database Entity
  -> Converter: Entity -> Response
  -> Service
  -> Controller / ApiReturn
```

调用方向硬约束（红线）：

- Controller 不得直接依赖 DAO、Mapper 或 Entity。
- Service 接口不得暴露 Entity。
- Query 不得直接下沉 DAO，必须先转换为 BO。
- DTO、Query、VO 不得互相替代。
- DAO 只返回 Entity、标量或内部分页结果，不返回 Web DTO。
- Entity 不得穿过 Controller。
- **不得跨 Service 直接访问别的实体的 DAO**：一个模块的 ServiceImpl 只能注入/调用与本模块实体对应的 DAO；跨服务、跨模块的交互**只能通过对方的 `service` 接口（或领域事件）**，禁止直接依赖其他模块的 DAO / Mapper。

## 4. 分层规范

以下为各层标准口径的压缩版，完整细则以 SPEC 为准（`xxx-xxx-xxx/DESCR.md` 是 SPEC §1–§10 的副本，可直接阅读）。参照样本：`DictData`（sys 模块）与 `Customer`（customer 模块）全链路。

### 4.1 Entity（`domain/entity/<模块>`）

- 表名优先 `@TableName`，缺省退化为 `xxx_` + 类名 snake_case；主键 `Long` + `@TableId(type = IdType.AUTO)`（数据库自增，**不使用雪花 ID**）。
- 系统字段六件套：`id / createdTime / createdBy / updatedTime / updatedBy / deleteStatus`。
- 抽象基类 `BusinessBaseEntity`（六件套）与 `BusinessNamedEntity`（追加 `status / name / sort`）可继承，**但非强制**——存量实体两种形态并存（如 `Customer` 即字段本地声明、未继承基类）。缺列的表禁止继承 `BusinessNamedEntity` 造出不存在的列。
- 逻辑删除**不挂 `@TableLogic`**：DAO 用带 `delete_status` 条件的显式更新实现。
- 审计字段**不用 `@FieldFill`**：由 Service 层显式填充。

### 4.2 模型边界：DTO / Query / BO / VO

| 模型 | 位置 | 职责 | 要点 |
| --- | --- | --- | --- |
| DTO | `domain/dto` | 管写（Controller → Service） | `XxxCreateRequest` 不含主键/审计字段；`XxxUpdateRequest` 必填主键，业务字段 null = 不修改；`XxxDeleteRequest` 承载主键（供 `/delete` 的 `@RequestBody` 入参）；NOT NULL/长度/精度生成 `@NotNull/@Size/@Digits` |
| Query | `domain/query` | 管读 | `XxxPageQuery extends QueryPage`（分页+筛选平铺）；`XxxDetailQuery` 只含必填主键；VARCHAR 默认转义 LIKE，数值/布尔/日期默认精确匹配 |
| BO | `domain/bo` | Service → DAO 内部查询 | `XxxPageQueryBO extends QueryPage`，非 HTTP 契约、不加校验注解 |
| VO | `domain/vo` | 只读响应 | `XxxResponse`；联表/聚合场景建专用 VO，不强行套用单表 Response |

- DTO 不承载分页/读筛选、不作响应、不进 DAO；Query 不用于写、不转 Entity；VO 不接请求、不进 DAO。敏感字段（password/secret/token/credential）不得出现在 VO。

### 4.3 Converter（`domain/convert/<模块>/XxxConverter`）

MapStruct，`@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = IGNORE)`。方法统一目标导向命名：

```java
Xxx toEntity(XxxCreateRequest request);
Xxx toEntity(XxxUpdateRequest request, @MappingTarget Xxx entity); // 更新，null 不覆盖
XxxResponse toResponse(Xxx entity);
List<XxxResponse> toResponseList(List<Xxx> entities);
XxxPageQueryBO toPageQueryBO(XxxPageQuery query);
```

- 禁止 `convertXxx / mapXxx / buildXxx / updateEntity` 命名；不提供 DTO↔VO 直接转换。
- 主键、审计、删除标记字段 `@Mapping(ignore = true)`。
- 旧命名 `XxxConvert / XxxSaveRequest / XxxPageQueryRequest / XxxFilterRequest` 是 codegen 迁移产物，与新命名（entity-chain 口径）**勿混用**。

### 4.4 Mapper 与 XML

```java
@Mapper
public interface XxxMapper extends BaseMapper<Xxx> {
}
```

XML 仅作为 JOIN、聚合、派生列、自定义批量 SQL 的扩展点；namespace 必须与 Mapper 接口全限定名一致；复杂分页须同时审查查询 SQL 与 count SQL。

### 4.5 DAO（`dao/<模块>/XxxDao` + `impl/XxxDaoImpl`）

标准五方法：

```java
PageResult<Xxx> pageActive(XxxPageQueryBO query);
Xxx getActiveById(Long id);
boolean create(Xxx entity);
boolean update(Xxx entity);
boolean deleteById(Long id, LocalDateTime updatedTime, String updatedBy); // 逻辑删除
// 实体无 deleteStatus 时退化为 boolean deleteById(Long id) 物理删除兜底
```

DAOImpl 规则：`@Repository`，继承 `CrudRepository<XxxMapper, Xxx>`；只用 `LambdaQueryWrapper`（禁止字符串拼列名）；模糊查询统一走 `LikeEscapeUtil.like`（禁止手写 LIKE）；分页用 `PageUtils.buildPageWithoutSort` + `PageUtils.toPageResult`；有 `deleteStatus` 时查询统一过滤 `FALSE`，删除用 `LambdaUpdateWrapper` 显式置 `true` 并写入 `updatedTime/updatedBy`，零行更新返回 `false`；默认排序 `createdTime DESC`（无该字段则主键倒序）；DAO 不承载事务与业务校验。

### 4.6 Service（`service/<模块>/XxxService` + `impl/XxxServiceImpl`）

接口只暴露 DTO/Query/VO/PageResult/主键/布尔，标准五方法：

```java
PageResult<XxxResponse> page(XxxPageQuery query);
XxxResponse detail(XxxDetailQuery query);
Long create(XxxCreateRequest request);
boolean update(XxxUpdateRequest request);
boolean delete(Long id);
```

实现规则：`@Service` + `@RequiredArgsConstructor`（禁止字段注入）；**查询方法默认不加 `@Transactional`**，写方法 `@Transactional(rollbackFor = Exception.class)`；PageQuery 先经 Converter 转 BO 再进 DAO；create 由 Converter 新建 Entity、Service 显式填充审计字段与 `CurrentUser.username()`、初始 `deleteStatus=false`；**update 必须先 `getActiveById` 加载现有 Entity 再 `toEntity(request, entity)`**，禁止新对象整体覆盖；delete 前校验 `CurrentUser.username()` 非空，否则抛 `ServiceException("无法获取当前操作人")`；持久化失败或目标不存在抛 `ServiceException`。

### 4.7 Controller（`controller/<模块>/XxxController`）

- `@RestController` + `@RequiredArgsConstructor`，根路由 `/api/<模块>/<资源kebab>`。
- **包路径与路由前缀允许偏离**：合同域（`contract`）的端点根路由保留历史前缀 `/api/finance/*` 与 `/api/customer/strategic-agreement`、`/api/customer/level/sync/*`，因 OA 流程配置与前端调用把这些 URL 作硬编码依赖；迁模块只动包路径不动路由，被迁移的 Controller 须在类级 Javadoc 写明这一偏离。新增合同域端点应直接落在既有前缀下，不要为一「新」资源另立 `/api/contract/*` 造成同类资源两套前缀。
- 固定五端点，**只用 GET/POST**（禁止 PutMapping/DeleteMapping/PatchMapping）：

| 端点 | 方法 | 入参 |
| --- | --- | --- |
| `/page` | POST | `@Valid @RequestBody XxxPageQuery` |
| `/detail` | GET | `@Valid @ModelAttribute XxxDetailQuery` |
| `/create` | POST | `@Valid @RequestBody XxxCreateRequest` |
| `/update` | POST | `@Valid @RequestBody XxxUpdateRequest`（含 id） |
| `/delete` | POST | `@Valid @RequestBody XxxDeleteRequest`（含 id） |

- **所有 POST 端点统一使用 POST，且必须定义并使用对象类型作为 `@RequestBody` 入参**：不限于固定五端点里的 page/create/update/delete，新增的任何 POST 类端点（批量操作、状态流转、审批、导入导出、触发外部调用等自定义动作端点）一律如此；不得用 `@RequestParam` 平铺标量或 path 变量传参（删除用专用的 `XxxDeleteRequest` 承载 `id` 及必要的校验/审计字段）。其中 `/page` 用分页查询对象 `XxxPageQuery` 作为 `@RequestBody` 入参（分页+筛选条件较复杂，统一走 POST + JSON body）；`/detail` 及新增简单读端点仍为 GET + `@ModelAttribute XxxQuery`。

- 每个方法带 `@Operation`（summary 与 description 用同一中文文案）；返回统一 `ApiReturn<T>`（`{code, data, msg}`，code=0 成功，`ApiReturn.of(data)`），分页用 `PageResult<T>`。
- Controller 只做协议适配、校验和响应包装，不写业务逻辑。
- **OA 入站 Controller 标 `@OAInbound`**（`base/annotation/OAInbound`）标识调用方向 OA → OMS：仅类级，一个 Controller 整体属同一方向，出现「入站 + 站内」混合端点时按方向拆分 Controller，不下沉到方法级；`value` 写一句入站场景简述。该注解**无任何运行时行为**（无切面/无拦截器/不参与鉴权与路由），不得把它当作免鉴权凭据；OMS → OA 的出站调用走 `extapi/oa` 客户端、不标本注解；WMS/CRM 等其它外部入站暂不纳入，需要时另立注解，不与 `@OaInbound` 混用。当前标注范围：`OaCallbackController`、`QuotaOaSyncController`。

### 4.8 操作人与审计上下文

`CurrentUser`（`base/support`）是迁移期占位实现（ThreadLocal 持有），当前无调用链写入上下文时 `username()` 返回 null——create/update 的审计用户名可能为 null，逻辑删除则因空操作人直接失败。接入方须在业务调用前通过统一鉴权上下文设置 `CurrentUser`。

### 4.9 注释与作者标识

- **新增编码的所有注释中 `@author` 统一设为 `Punjab`**：新建的类/接口/枚举等类型级 Javadoc 一律写 `@author Punjab`（与仓库现存生成产物如 `DictDataCreateRequest`、`DictTypeController` 一致）。
- 适用范围：所有新增代码——含手写业务类、DTO/Query/BO/VO、Converter、DAO、Service、Controller、配置与测试类等；codegen 生成的新文件同样以 `@author Punjab` 产出。
- 仅当在已有文件上新增类型（如内部类、新增独立类）时，该新类型的 `@author` 取 `Punjab`；不改动同文件中原有的 `@author`。
- 例外：修改存量代码、局部方法/字段注释等不强制新增 `@author`；本规则只约束**新增编码的 `@author` 取值**，不得为历史文件回溯批量替换。

## 5. 测试规范

每次进行**功能新增、功能调整、需求变更、缺陷修复或接口行为改动**时，必须同步编写、补充或更新相应的测试用例，并以「测试通过」作为功能完成与合并准入的必要条件。本规则与 §4 分层规范、§6 构建验证、§7 Git 分支协作一致，是开发与代码评审时的硬约束。

用例与业务代码**同批提交**（同一分支、同一 PR），不接受「先合码、后补测试」；缺陷修复须先有一条能复现该缺陷的失败用例，再改代码使其通过。

### 5.1 适用范围

任一层的业务改动均须配套测试，包括但不限于：

- 分层代码：Controller、Service / ServiceImpl、DAO / DAOImpl、Mapper（接口与 XML）、Converter、Entity、DTO / Query / BO / VO。
- 行为相关变更：数据库字段与迁移脚本（`scratch/dbddl/`、表结构变更）、权限控制、审计字段填充（`CreatedBy/UpdatedBy/UpdatedTime`、`CurrentUser`）、业务校验（`@NotNull/@Size/@Digits`、Service 层前置校验）、异常处理（`ServiceException` 的抛出与文案）。
- 计算与规则逻辑：金额/价格/折扣/额度等口径类算法、状态机流转、幂等与并发（CAS）路径、逻辑删除后数据不可被查回。
- 对外契约：接口入参/出参/状态码与 `ApiReturn` 结构变化、extapi 客户端（hkerp/oa/wms/auth）报文组装与回调处理、Job 与同步链路。
- 生成器：codegen 模板与 `codegen_tool/specs/*.gen.yaml` 变更（配套更新 `codegen_tool/tests` 的 Python 用例）。

纯文档、注释、日志文案、格式化等**不改变运行时行为**的改动可不新增测试（见 5.4 例外）。

### 5.2 测试要求

- **分层与位置**：测试镜像主源码包结构，放在 `xxx/src/test/java/com/xxx/sys/erp/oms/<同层包>/`，常规用例类名以 `Test` 结尾（如 `OrderCancelServiceImplTest`）；实库联测命名后缀口径见下「实库集成测试」条。
- **技术栈**：JUnit 5（`@Test` + `@DisplayName` 描述场景）+ Mockito（`@ExtendWith(MockitoExtension.class)`、`@Mock`，ServiceImpl 在 `@BeforeEach` 手动构造注入）+ AssertJ（`assertThat` / `assertThatThrownBy`；金额用 `isEqualByComparingTo` 断言，勿对 `BigDecimal` 用 `equals`）。test 依赖仅 `spring-boot-starter-test`、`mockito-junit-jupiter`、`assertj-core`、`h2`（均已在 `xxx-xxx-xxx/pom.xml` 声明），**不新增其他 test 依赖**；单表 SQL 可用 H2，但涉及 `delete_status` 过滤、`LikeEscapeUtil` 转义、分页 count 与查询一致、零行更新返回 `false` 等语义时以真实库联测为准。参照样本：`CustomerAccountBalanceServiceIncomeTest`（门禁+幂等+流水一致性）。
- **覆盖场景**：新增或调整功能时，至少覆盖——
  - 核心业务路径（happy path，含真实返回字段与落库/持久化副作用的断言，用 `verify(...)` 校验下游 DAO/端口调用）；
  - 关键分支（if/else、状态流转、开关）；
  - 边界条件（空值/null、零/负值、精度与上限、分页边界）；
  - 异常输入（校验不通过的 DTO、目标不存在、非法状态），断言异常类型与 `ServiceException` 文案；
  - 必要时的并发/幂等/权限/数据校验（如 CAS 未命中抛刷新提示、重复请求早退、操作人为空失败）。
- **回归测试**：对回归风险较高的改动（金额/价格恒等式、结算、取消/撤回链路、出数落定、折扣引擎等等价性敏感逻辑）应补充回归用例，锁定既有行为（如等价性对比 `*EquivalenceTest`、基线 `*BaselineTest`）。
- **实库集成测试**：依赖 UAT/Nacos/真实数据库或外部系统的联测（类名后缀 `RealTest` / `IT` / `ProbeTest` / `SmokeTest` / `UatPersistTest`）用 `@SpringBootTest(classes = ApiServerErpApplication.class)` + `@ActiveProfiles("pre")` + 关闭服务注册（`spring.cloud.service-registry.auto-registration.enabled=false`），并默认门禁避免本地/无内网环境误跑：用 `@EnabledIfSystemProperty(named = "<域>.<场景>.real", matches = "true")`（仓库现行主要写法）或 `@Disabled`；**类 javadoc 必须写明启用命令**，例如 `mvn test -Dtest=WmsAuthConnectivityTest -Dwms.connectivity=true`。会真实写库或写外部系统的分支再套一层更窄的开关（参考 `cancel.settle.real.commit`），跑完恢复禁用。`@Tag` 分组需先在模块 pom 的 surefire 配置 `<groups>`（当前未配置），勿依赖。
- **默认可跑性**：`mvn -pl xxx-test` 必须无外部依赖、可重复、全绿；surefire 3.2.5 只收集 `Test*` / `*Test` / `*Tests` / `*TestCase`，`*IT` 不随 `mvn test` 执行。

### 5.3 完成标准

- 用例编写完成后，必须执行对应模块或项目级测试命令并**全部通过**：

```bash
mvn -pl xxx-xxx-xxx test                   # 单模块测试（不含实库门禁用例）
mvn test                                  # 全量测试
mvn -pl xxx-xxx-xxx test -Dtest=XxxTest    # 定向执行（如 OrderCancelServiceImplTest），随后补模块级全量
```

- 「功能完成」的定义：改动层有对应新增/更新用例，且上述测试命令本地绿跑。
- 「合并准入」的硬门槛：在 §7「合并前人工准入」清单基础上，增加「相关测试已通过」一项——**无法通过测试的改动不得进入 `feature/*`→`release/*` 合并，更不得进入发布流程**；缺陷修复应先补一个能复现该缺陷的失败用例，再修复使其通过。
- 不得以注释断言、给既有失败用例加 `@Disabled` 等方式换取绿灯；确需临时禁用，须在提交说明中写明原因与恢复条件。
- 当前环境确实无法执行测试时（缺内网 Nexus、surefire 插件依赖不可达等），须在 PR/提交说明中写明：未执行的命令、失败原因、替代验证方式（IDE 运行同一批用例、内网机器补跑）与补跑责任人。

### 5.4 例外处理

确不适合编写自动化测试的改动（如纯展示文案、外部系统真实连通性无法稳定 mock、一次性数据修补脚本），须同时满足：

1. 属于 5.1 中「不改变运行时行为」或明确标注为手动联测的实库场景；
2. 在 **PR / 提交说明**中记录：不适用自动化测试的原因，以及**人工验证方式与验证结果**（验证步骤、环境、预期/实际）。

未记录例外说明的「跳过测试」不视为合规；「没时间」「太简单」「后面补」不构成例外理由，评审时应直接打回。`hotfix/*` 不豁免——至少须包含一条复现该缺陷的用例，并与修复同批提交。

留痕模板（写在 PR 描述或提交说明中）：

```text
测试例外：<改动范围>
原因：<为何不适合自动化>
人工验证：<环境 + 步骤 + 数据 + 结果>
后续补齐：<是否补用例 + 责任分支或时间点>
```

## 6. 构建与验证

```bash
mvn -pl xxx-xxx-xxx compile   # 单模块编译
mvn compile                  # 全量编译
mvn -pl xxx-xxx-xxx test      # 单模块测试
mvn test                     # 全量测试
```

- Java 侧测试位于 `xxx-xxx-xxx/src/test/java/com/xxx/sys/erp/oms/`（JUnit 5 + Mockito + AssertJ，H2 可选），随功能迭代持续补充；实库集成用例默认门禁（`@Disabled` / `@EnabledIfSystemProperty`），不在常规 `mvn test` 中执行。测试编写与准入要求见 §5。
- 生成器的 Python 单测在 `codegen_tool/tests`（`pyproject.toml` 已配 `testpaths`，须在 `codegen_tool/` 目录下用 `py -3 -m pytest` 运行）。
- 依赖内网 Nexus 与 BOM SNAPSHOT：三方依赖已缓存在本地 `.m2` 时，`mvn -pl xxx-xxx-xxx compile` 可离线（`-o`）通过；`mvn test` 还需 surefire 插件自身依赖可达，无内网且未缓存时会失败——此时按 §5.3 末条留痕，不得免测。
- Maven 4.x 对 HTTP 仓库有 blocker，需企业镜像规则或升 HTTPS。

## 7. Git 分支协作

依据《巨丰IT系统开发—Git分支协作管理规范V1.0》。本仓库生产基线为 **master**（规范原文用 main 指代生产基线，适用于 master 仓库时全部等价替换）；暂无 CI/CD 与 develop 集成分支，合并准入靠人工检查。完整工作流命令、异常处理与清理规则见 skill `.claude/skills/git-branching/`。

| 对象 | 用途 | 从哪创建 | 合并目标 | 生命周期 |
| --- | --- | --- | --- | --- |
| `master` | 生产基线，只保存已发布版本 | — | 只接收 `release/*`、`hotfix/*` | 长期 |
| `vX.Y.Z` tag | 生产发布不可变标识 | 只打在 master 发布提交 | — | 永久，禁删改复用 |
| `release/vX.Y.Z` | 测试区与发布候选 | master | 合回 master；接收 `feature/*`、`bugfix/*` | 发布过观察期后冻结/删除 |
| `feature/<scope>` | 新功能 | 目标 `release/*`（已规划）或 master（需求先行） | 目标 `release/*`，禁直接合 master | 版本发布完成后删除 |
| `bugfix/<scope>` | 测试/release 阶段缺陷 | 当前 `release/*` | 当前 `release/*` | 回归通过且发布完成后删除 |
| `hotfix/<scope>` | 生产紧急修复 | 最新生产 tag（优先）或 master | master + 补丁 tag + 同步活跃 `release/*` | 同步完成后删除 |

命名：`release/*` 必须带版本号；`feature/*` 必须表达需求/任务范围（禁止 `feature/test`、`feature/tmp`、`feature/new`）；`bugfix/*` 推荐 `bugfix/vX.Y.Z-<problem>` 或 `bugfix/BUG-<id>-<problem>`；`hotfix/*` 推荐 `hotfix/prod-<problem>` 或 `hotfix/INC-<id>-<problem>`；tag 用语义化版本（常规 v1.2.0、补丁 v1.2.1）。

红线：

- 禁止直接在 master、`release/*` 上提交业务代码；master 只经合并 `release/*` 或 `hotfix/*` 变更。
- `feature/*`、`bugfix/*` 不得直接合入 master；需求先行 feature 也不得直接打 tag 发布。
- 生产发布必须在 master 打 tag；tag 禁止删除、改写、复用、强推。
- 合并一律 `--no-ff`；禁止 `reset --hard` 后 force push 到 master / `release/*`。
- **测试准入**：未按 §5 配套用例、或 `mvn -pl xxx-xxx-xxx test` 未通过的改动，禁止合入 `release/*`、`master`，禁止纳入 tag；例外须按 §5.4 留痕，`hotfix/*` 亦不豁免。
- 合并前人工准入：工作区干净、目标分支正确、已拉取最新、本地可编译启动（`mvn -pl xxx-xxx-xxx compile`）、**相关测试已通过（`mvn -pl xxx-xxx-xxx test`，见 §5）**、核心路径人工验证。

现状注记：仓库现存 `dev` 分支（规范模型无 develop，属待收敛遗留分支，勿从 dev 切新分支）与 `codex/*` 非受控分支；尚无任何生产 tag 与 `release/*`，首次发版按 skill 的 W0 流程开始。
