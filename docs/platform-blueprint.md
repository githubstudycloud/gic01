# platform-* 企业级 Spring Boot 4.x 基座蓝图

本文面向“可作为其他项目嵌入的基座（platform-*）”设计：严边界、强治理、可验证、可部署、可观测、可持续扩展。

> 说明：Spring Boot 4.x 的具体依赖矩阵（JDK/Spring/云原生组件）会随时间变化。为降低升级成本，本文将框架/中间件影响隔离在 `platform-starter-*` 与 adapter 层，业务域模型尽量保持不动。

## 设计目标

- 所有模块以 `platform-` 开头，模块职责清晰、可单独验证、可替换实现
- 构建与依赖：优先 Maven；同时保证 Gradle 项目可无痛消费（BOM/版本目录/示例）
- “最小化发布验证”：变更集最小验证链路 + 失败即停（fail-fast）
- “独立模块验证”：每个 starter/adapter 都可在最小上下文启动并通过自身测试
- “集群并发验证”：可复现实验场 + 固化并发/一致性/故障场景
- “一键多方式部署”：同一产物可投放 Docker/Compose/K8s/裸机
- “自监听+汇总”：服务自带可观测输出；可选集中汇总 Hub；也可作为 SDK 嵌入到其他项目
- “依赖易升级替换”：统一 BOM；SPI/adapter 解耦；可并存多实现；升级只替换 platform 壳
- “依赖三年规则”：默认仅允许 36 个月内发布的依赖；超期依赖 vendor 到仓库内本地 repo/lib，便于自维护

## Java 基线（17 + 可选 21）

- 最低支持：JDK 17（默认编译产物以 `--release 17` 输出，确保可在 17+ 运行）
- 推荐运行：JDK 21（生产默认 21）
- 可选开启 21 特性：提供 `java21` 构建 profile（或等价机制），在需要时将编译目标切到 21；此时运行时也要求 21
- 原则：平台核心（`platform-kernel`、domain）尽量保持 17 兼容；“明确依赖 21 的能力”（如 virtual threads 的默认启用策略）封装在可选 starter/配置开关内

## 构建策略（优先 Maven，兼容 Gradle 消费）

“支持 Gradle”的推荐解释：平台仓库以 Maven 作为唯一权威构建（减少双构建系统漂移风险），但发布的产物/约束对 Gradle 项目同样友好：

- Maven 侧：
  - `platform-parent`：统一插件与门禁
  - `platform-bom`：统一依赖版本（业务禁止写版本号）
- Gradle 侧（消费与落地）：
  - 提供 BOM 的 Gradle 引用示例（`platform(...)` / `enforcedPlatform(...)`）
  - 提供可选的 Version Catalog（`libs.versions.toml`）用于 Gradle 项目对齐依赖坐标与版本

> 如果你们确实需要“同一仓库同时可用 Maven/Gradle 构建平台源码”，也可以做，但维护成本会显著上升；建议先按 Maven 权威落地，后续再评估是否引入并行 Gradle build。

## 仓库分层与命名约定

建议按“平台层（foundation）/业务层（business）/工具层（tooling）”组织，但都以 `platform-` 前缀开头。

### 平台层（foundation）

- `platform-parent`：统一构建与质量门禁（compiler/enforcer/format/test profiles/SBOM 等）
- `platform-bom`：唯一第三方依赖版本入口（业务模块禁止显式写版本号）
- `platform-kernel`：最小内核（错误码/异常模型/结果模型/ID/时间/序列化约定）；严禁 Spring 依赖
- `platform-spi-*`：端口层（cache/mq/blob/lock/notify/search/featureflag...），只放接口与 DTO
- `platform-adapter-*`：SPI 具体实现（redis/kafka/s3/es...），可替换/可并存
- `platform-starter-*`：Spring Boot 自动装配与平台约定（web/security/persistence/reliability/observability...）
- `platform-observability-hub`（可选应用）：集中汇总与总览（版本/健康/依赖矩阵/指标聚合等）

### 业务层（business）

每个 bounded context（或服务）建议固定四段式模块（仍以 `platform-` 开头）:
- `platform-<ctx>-domain`：领域模型（纯 POJO、规则、值对象、聚合）
- `platform-<ctx>-application`：用例编排（事务边界、调用 SPI、发布领域事件）
- `platform-<ctx>-adapter`：当前业务上下文特有的 adapter（如对接旧系统/第三方）
- `platform-<ctx>-entry`：应用入口（Spring Boot Application + controller/consumer/scheduler）

> 若采用微服务：`platform-<svc>-entry` 产出运行制品；其余模块可被测试与复用。

### 工具层（tooling）

- `platform-test-*`：测试基建（ArchUnit 规则、Testcontainers 封装、契约测试工具等）
- `platform-loadtest-*`：并发/性能脚手架（k6/Gatling 场景与数据生成）
- `platform-deploy-*`：多方式部署封装（Docker/Compose/Helm/systemd + 一键入口脚本）
- `platform-thirdparty-repo`：仓库内本地依赖仓（vendor 的 Maven repo 布局或等价方案）
- `platform-deps-metadata`：依赖元数据（发布日期/来源/许可/风险/替换计划）

## 最严格的“依赖与边界”规则（强制落地）

### 层级依赖白名单（建议用 ArchUnit + 构建门禁）

- `domain`:
  - 允许：JDK、`platform-kernel`、少量纯函数库（可选）
  - 禁止：Spring、ORM、HTTP/MQ 客户端、线程池/调度框架直接依赖
- `application`:
  - 允许：`domain`、`platform-spi-*`、`platform-kernel`
  - 禁止：直接依赖具体 adapter；禁止“拿到客户端就用”（必须通过端口）
- `adapter`:
  - 允许：具体中间件客户端（DB/MQ/Redis/ES...）、`platform-spi-*`
  - 责任：协议/序列化/重试/超时/幂等等非业务细节
- `entry`/`starter`:
  - 允许：Spring Boot、Web/MQ Listener、配置绑定、装配与拦截器
  - 禁止：写业务规则（业务规则必须在 domain/application）

### “业务不触达框架”落地手段

- 业务只引入 `platform-starter-*` 与自己的 `<ctx>-domain/<ctx>-application`
- 任何中间件替换，改的是 `platform-adapter-*` 或某个 starter 的装配，而不是业务代码

## 最小化发布验证（Change-based Verification）

核心思路：验证链路可组合、可裁剪；PR 上只跑“变更影响集”的最小集合；发布/夜间跑全量。

### 测试分层与标签

- `unit`：纯单测（毫秒~秒）
- `slice`：Web/JPA/MQ slice（秒~十秒）
- `it`：Testcontainers 集成（分钟级）
- `contract`：契约测试（OpenAPI/AsyncAPI/Schema 兼容性）（分钟级）
- `e2e`：端到端（可选，夜间/发布前）

### starter 的“最小启动测试”规范（必须）

每个 `platform-starter-*` 必须自带一个最小 demo app/测试：
- 仅引入该 starter + `platform-kernel`
- 仅提供最小配置
- 断言关键 bean/拦截器/过滤器/配置绑定存在

目的：升级 Spring Boot/依赖时，starter 第一时间爆炸（fail-fast），避免业务侧隐性回归。

## 独立模块验证（Module-level Verification）

每个模块至少具备以下之一：
- 纯单测（domain）
- 最小上下文启动测试（starter）
- Testcontainers 集成测试（adapter）
- 契约测试（api/消息 schema）

并要求：模块测试不依赖“全仓库启动一个巨型应用”。

## 集群与并发验证（可复现、可量化、可自动化）

### 并发/一致性基线（建议由 `platform-starter-reliability` 提供标准件）

- 幂等：幂等键规范、幂等存储（DB/Redis）可插拔
- Outbox/Inbox：事务一致性与“至少一次投递”的标准化落地
- 去重：消息重复投递处理（去重表/去重缓存/窗口策略）
- 重试预算（retry budget）：限制重试风暴；可观测并可告警
- 超时与退避：统一的超时、退避、熔断、限流策略（集中配置）

### 集群实验场（推荐）

- 本地：kind/k3d 一键起 3 副本 + HPA + 依赖组件（DB/MQ/Redis）
- 场景：滚动升级、Pod 重启、网络抖动、依赖不可用、热点 key、时钟漂移（必要时）
- 工具：k6/Gatling 固化脚本；Chaos 工具做故障注入

输出：稳定的指标与门槛（P95/P99、错误率、重试率、消息堆积、延迟抖动）。

## 一键多方式部署（One Artifact, Many Targets）

### 统一产物与配置策略

- 产物：默认 OCI 镜像（保留 `java -jar` 兼容）
- 配置：12-factor（env/configmap/secret），禁止把环境差异打进代码
- 健康与版本门禁：readiness/liveness/版本探针 + 数据库迁移策略（蓝绿/灰度）

### 一键入口（建议以脚本/CLI 形式提供）

提供统一命令：
- `deploy docker`（本机 Docker）
- `deploy compose`（本机多依赖）
- `deploy k8s`（Helm）
- `deploy baremetal`（systemd）

并内置：
- 部署前检查（镜像/配置/依赖可达性）
- 发布策略（蓝绿/金丝雀/灰度）
- 回滚（基于健康门禁自动回滚/手动一键回滚）

## 可观测：自监听 + 汇总 + 可嵌入

### `platform-starter-observability`（必须）

- 日志：结构化 JSON、统一字段（traceId/spanId/userId/tenantId/requestId/version）
- 指标：统一命名规范与标签；暴露 runtime/业务关键指标
- 链路：OTel tracing（采样/脱敏/导出可配置）
- 端点：health/info/build/version/config checksum（注意脱敏）

### `platform-observability-hub`（可选）

集中能力：
- 服务清单与健康矩阵、版本矩阵、依赖矩阵
- 指标聚合与告警模板
- 作为“可嵌入基座”：Hub 可独立运行，也可仅使用 starter 将数据输出到现有观测系统

## 依赖治理：三年规则 + 本地 vendor（可自维护）

### 规则

- 业务模块：禁止写版本号；只能通过 `platform-bom` 获取版本
- 默认只允许“发布 <= 36 个月”的依赖进入 BOM
- 超期依赖必须 vendor 到仓库内本地 repo/lib，并在元数据中登记

### 推荐的 vendor 方式（Maven 示例思路）

- 在仓库内维护 `platform-thirdparty-repo/`（Maven repo 布局：groupId/artifactId/version/...）
- 构建配置优先解析该 file-based repo（可离线构建、便于补丁自维护）
- `platform-deps-metadata/` 记录：来源 URL、发布日期、许可证、风险、替换计划、维护负责人

> 是否采用 Maven 或 Gradle 不影响上述原则；关键是“版本集中 + 门禁强制 + vendor 可审计”。

## 推荐的交付物（落地清单）

- 代码结构：按上述模块拆分，并加 ArchUnit 规则强制边界
- 构建门禁：依赖年龄/来源、漏洞阈值、许可证、SBOM、禁止快照依赖
- 测试门禁：最小启动测试（starter）、adapter IT、契约测试、可选集群/并发验证
- 部署封装：同一套命令覆盖 Docker/Compose/Helm/systemd
- 可观测：starter 即插即用，Hub 可选集中

## 预发布命名占位（可一键替换）

- 预发布阶段可先使用占位 groupId（如 `com.test.platform`）避免暴露公司真实命名空间。
- 建议将 groupId 只放在根 POM，模块继承；并提供脚本一键替换 groupId 与 Java base package，降低后续迁移成本。

## 下一步（建议你确认两点后我可以把仓库骨架直接生成出来）

1) 你希望构建工具是 Maven 还是 Gradle？（BOM/本地 repo 的落地方式会略有差异）
2) 你期望的运行时基线：JDK 版本、是否强制 K8s、是否需要 Spring Cloud/Service Mesh？
