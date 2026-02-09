# QA Archive / QA 存档

This file records user questions and a brief summary of answers.
Append a new entry at the end after each session.

本文件用于记录用户问题与回答要点；每次会话结束后在末尾追加一条记录。

## 2026-02-09 - 企业级 Spring Boot 4.x 平台化蓝图（platform-*）

Q（用户需求摘要）:
- 设计企业级 Spring Boot 4.x 基座，所有模块以 `platform-` 开头，架构约束要“最严格”
- 最小化发布验证、支持独立模块验证
- 支持集群并发验证
- 一键式、多方式部署
- 自监听与汇总，可作为其他项目可嵌入的基座
- 支持持续横向扩展业务、依赖易升级/替换、尽量解耦
- 依赖治理：三年内发布的依赖优先；超过三年的依赖从本地 lib/repo 加载，便于自维护

A（回答要点）:
- 架构：DDD + Hexagonal（领域纯 POJO；应用层编排；adapter 才接触 DB/MQ/HTTP；Spring 约束在 starter/entry）
- 模块：`platform-kernel`（无 Spring）、`platform-spi-*`（端口）、`platform-adapter-*`（实现可替换）、`platform-starter-*`（自动装配与标准）、`platform-bom`/`platform-parent`（依赖与构建治理）
- 验证：按 profile/tag 组合最小验证链路（starter 最小启动测试、slice、Testcontainers 集成、契约测试、可选 e2e/nightly）
- 并发/集群：可复现本地 K8s 实验场（kind/k3d）+ 压测（k6/Gatling）+ 故障注入；统一幂等、outbox/inbox、去重、重试预算
- 部署：同一产物多投放（OCI 镜像 + `java -jar`），封装 Docker/Compose/Helm/裸机 systemd 一键入口；用健康/readiness/版本探针做发布门禁
- 可观测：可嵌入 `platform-starter-observability`（logs/metrics/traces/health/build info），可选 `platform-observability-hub` 做集中汇总
- 依赖治理：BOM + 构建门禁强制“三年规则”；超期依赖 vendor 到仓库内 file-based repo（或等价方案）并记录元数据用于审计/替换计划

## 2026-02-09 - 构建工具与 Java 基线选择

Q（补充约束）:
- 是否可以同时支持 Maven 和 Gradle？要求优先 Maven、同时支持 Gradle
- JDK 最少支持 17+，生产通常运行 21；希望可选择开启 21 新特性

A（回答要点）:
- Maven 作为平台仓库“唯一权威构建”（降低双构建系统长期漂移风险），以 `platform-parent` + `platform-bom` 统一版本与门禁。
- “支持 Gradle”的落地优先按“可消费性”实现：发布 BOM 与 starter/jar 到 Maven 仓库，提供 Gradle 引入 BOM（`platform/enforcedPlatform`）与可选 Version Catalog（`libs.versions.toml`）示例，保证 Gradle 项目可无痛对齐依赖。
- Java 策略：默认用 `--release 17` 构建确保 17+ 运行；生产推荐 21；提供可选 `java21` profile 将编译目标切到 21（需要 21 运行时），并将依赖 21 的能力封装在可选 starter/开关中。

## 2026-02-09 - 预发布占位 groupId 与可替换方案

Q（补充约束）:
- 预发布期间先用 `com.test.platform` 作为 groupId（避免暴露公司真实命名）
- 要求后续可以非常方便地整体替换 groupId/包名

A（回答要点）:
- Maven 侧只在根 `pom.xml` 定义 groupId，所有模块继承，降低替换成本到“改一个点”。
- 同步提供一键重命名脚本，将 groupId 与 Java base package（默认同值）全仓替换，并移动 `src/main/java`、`src/test/java` 目录结构，减少人工改动与遗漏风险。

## 2026-02-09 - 初始化 platform-* 仓库骨架（Maven 权威 + Gradle 消费示例）

Q（落地动作）:
- 以 `com.test.platform` 初始化平台仓库骨架（Maven 多模块），并提供 Gradle 消费示例与 Java17/Java21 profile。

A（结果摘要）:
- Maven 多模块：`platform-root`（聚合）、`platform-parent`（统一插件/门禁/Java17+ & 可选 java21）、`platform-bom`（导入 Spring Boot 4.x BOM + 管理平台模块版本）、`platform-kernel`（无 Spring 内核）、`platform-autoconfigure-observability` + `platform-starter-observability`（最小自监听能力示例）。
- 最小验证：默认 `mvn test` 仅跑单测；`-Pit` 开启 IT（当前作为骨架已预留 failsafe 配置）。
- Gradle 消费：`platform-example-gradle-consumer/` 使用 Gradle Wrapper + 导入 `platform-bom` 进行依赖对齐，并可直接跑测试验证可消费性。

## 2026-02-09 - 补全 SPI/Adapter 示例：分布式锁（local + redis）

Q（落地动作）:
- 按顺序补全 `platform-spi-*` 与 `platform-adapter-*` 示例，要求可替换实现、可独立验证、与 starter 解耦。

A（结果摘要）:
- 新增 SPI：`platform-spi-lock`（纯 Java，定义 `LockClient`/`LockRequest`/`LockHandle`，并用 ArchUnit 禁止依赖 Spring/Jakarta）。
- 新增 adapter：
  - `platform-adapter-lock-local`：本地 JVM 互斥锁实现（默认非可重入语义）。
  - `platform-adapter-lock-redis`：Redis 实现（SET NX PX + Lua compare-and-del 解锁），并提供可选 IT（Docker 不可用则自动跳过）。
- 新增 autoconfigure + starter：
  - `platform-autoconfigure-lock` + `platform-starter-lock`：默认 `platform.lock.provider=local`。
  - `platform-autoconfigure-lock-redis` + `platform-starter-lock-redis`：当 `platform.lock.provider=redis` 且存在 `StringRedisTemplate` 时装配 Redis 实现。

## 2026-02-09 - 补全 platform-deploy：一键多方式部署（docker/compose/k8s/systemd）

Q（落地动作）:
- 按顺序补全一键式多方式部署能力，并尽量最小化发布验证成本。

A（结果摘要）:
- 新增 `platform-deploy/` 的可复用部署骨架：
  - `platform-deploy/deploy.ps1` / `platform-deploy/deploy.sh`：支持 `docker|compose|k8s|baremetal` 四种模式的一键部署入口。
  - `platform-deploy/verify-http.ps1` / `platform-deploy/verify-http.sh`：部署后以 HTTP health URL 做最小发布验证（默认 readiness 探针）。
  - Dockerfile / Compose / Helm Chart / systemd 模板：`platform-deploy/docker/`、`platform-deploy/compose/`、`platform-deploy/helm/`、`platform-deploy/systemd/`。
- 新增 `platform-sample-app`：用于验证 starter 与部署模板的最小可运行样例（含 `/demo/ping`、`/demo/lock` 与 actuator probes）。
