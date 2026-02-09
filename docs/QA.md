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

## 2026-02-09 - 补全 observability-hub：服务自监听汇总（可选独立运行）

Q（落地动作）:
- 按顺序补全 `platform-observability-hub`，用于汇总各服务 actuator 输出（health/info），作为可选独立服务运行。

A（结果摘要）:
- 新增 `platform-observability-hub-core`：纯库层聚合能力（基于 Java `HttpClient` 拉取 `/actuator/health[/readiness]` 与 `/actuator/info`，并行抓取、超时控制、输出 `ServiceSnapshot`）。
- 新增 `platform-observability-hub`：Spring Boot 应用层，对外提供：
  - `GET /hub/services`：列出被观测服务清单（来自配置 `platform.hub.services`）
  - `GET /hub/snapshot`：并行抓取并返回快照列表（含 health/info、状态码、错误信息）

## 2026-02-09 - 补充需求纳入：新手引导/文档版本/统一规范/链路追踪与日志/团队人机协作

Q（新增约束/目标）:
- 增加项目新手引导
- 文档版本管理（可回溯、可审计）
- 链路追踪、统一日志管理查看、本地调试轻量化、系统状态监控、统一代码规范
- 前端与后端接口注册/通信管理（后续要做）
- Vue 框架（后续要做）
- Python 测试框架（后续要做）
- 团队成员与多个 AI 协作同一仓库

A（本次已落地）:
- 新手引导/协作治理：
  - `docs/getting-started.md`：10 分钟上手（构建/运行/验证/替换命名空间）
  - `AGENTS.md`：人+AI 协作规则（含必须追加 `docs/QA.md` 存档）
  - `CONTRIBUTING.md`：模块边界、验证链路、依赖治理原则
- 文档版本管理：
  - `docs/doc-versioning.md`：文档随代码发布的版本化策略
  - `docs/adr/`：ADR 模板与规范
  - `docs/releases/` + `CHANGELOG.md`：发布说明与变更摘要骨架
- 统一代码规范（轻量但可强制）：
  - `.editorconfig` + Maven `spotless`（validate 阶段检查，`mvn spotless:apply` 修复）
  - GitHub Actions：`ci.yml`（JDK 17/21 跑 `mvn test`）+ `nightly.yml`（`-Pit verify`）
- 观测（链路追踪/日志/状态监控）：
  - `platform-starter-logging` + `platform-autoconfigure-logging`：默认结构化 JSON 日志（可用 `platform.logging.enabled=false` 关闭）
  - `platform-starter-tracing-otel` + `platform-autoconfigure-tracing-otel`：提供 Micrometer Tracer + OTel SDK/OTLP exporter + 观测 handler 绑定
  - `platform-starter-observability` 聚合 logging + tracing + Prometheus registry（`/actuator/prometheus`）
  - 本地观测栈：`platform-deploy/compose/docker-compose.observability.yml`（Grafana + Prometheus + Tempo + OTLP collector）

A（后续待补全的方向）:
- 前后端接口注册/通信管理：OpenAPI 合同/registry/生成 TS client + 契约测试（计划引入）
- Vue 脚手架：Vue3 + Vite + TS + 生成的 API client（计划引入）
- Python 测试：pytest/httpx（黑盒 + 契约 + 并发），与 `platform-loadtest`（k6/Gatling）协同（计划引入）

## 2026-02-09 - 前后端接口注册/通信管理：OpenAPI 合同 + API Registry

Q（落地动作）:
- 将“前端与后端接口注册/通信管理”纳入基座设计，要求可最小化验证、可独立模块验证、可扩展。

A（本次已落地）:
- OpenAPI 合同约定：
  - 约定每个服务对外暴露稳定的 OpenAPI URL（默认 `/openapi.yaml`）
  - `platform-sample-app` 已提供静态合同：`platform-sample-app/src/main/resources/static/openapi.yaml`
- API Registry（可选独立服务）：
  - `platform-api-registry-core`：无 Spring 的核心抓取/摘要逻辑（HttpClient + 并行抓取 + sha256 摘要）
  - `platform-api-registry`：Spring Boot 应用（8082），对外提供：
    - `GET /registry/apis`：服务清单与 spec URL
    - `GET /registry/snapshot`：并行抓取快照（HTTP 状态 + digest）
    - `GET /registry/spec/{name}`：代理返回原始 spec
- 使用说明文档：`docs/api-contracts-and-registry.md`

## 2026-02-09 - Vue 框架：前端工程骨架 + OpenAPI 类型化调用

Q（落地动作）:
- 引入 Vue 前端框架，并与 OpenAPI 合同打通，支持本地轻量调试与统一请求关联。

A（本次已落地）:
- 新增前端工程：`platform-frontend-vue/`（Vue3 + Vite + TypeScript）
- OpenAPI 类型化调用：
  - `npm run gen:api` 从 `platform-sample-app` 的 `/openapi.yaml`（仓库内静态文件）生成 `src/api/openapi.ts`
  - 使用 `openapi-fetch` 进行类型化请求
- 通信/关联约定：
  - 前端请求自动附带 `X-Request-Id`（便于与后端日志/链路关联）
  - Vite dev server 默认 proxy 到 `http://localhost:8080`（减少 CORS/环境配置成本）
- 文档补充：`docs/getting-started.md` 已加入 Vue 启动步骤

## 2026-02-09 - Python 测试框架：pytest/httpx 黑盒 + 合同校验 + 并发用例

Q（落地动作）:
- 引入 Python 测试框架，用于黑盒验证、合同（OpenAPI）校验、以及轻量并发验证。

A（本次已落地）:
- 新增：`platform-test-python/`
  - 依赖：`pytest` + `httpx` + `pytest-asyncio` + `pyyaml` + `ruff`
  - 用例：
    - `test_smoke.py`：/demo/ping、/demo/lock 黑盒 smoke（含 `X-Request-Id` 断言）
    - `test_openapi.py`：校验 `/openapi.yaml` 存在且包含关键 paths
    - `test_concurrency.py`：并发 ping 用例（标记为 `integration`）
  - 通过 `PLATFORM_BASE_URL` 环境变量切换被测服务地址
- 文档补充：`docs/getting-started.md` 已加入 Python 测试启动步骤

## 2026-02-09 - 并发/集群验证：platform-loadtest（k6 + kind）

Q（落地动作）:
- 补全 `platform-loadtest`，用于最小化的并发/集群验证（可复现、可量化），并保持默认 PR 验证不膨胀。

A（本次已落地）:
- k6（Docker 运行，无需本地安装 k6）：
  - `platform-loadtest/k6/ping-smoke.js`：/demo/ping smoke
  - `platform-loadtest/k6/lock-contention.js`：锁争用场景（可配置 holdMillis）
  - `platform-loadtest/run-k6.ps1`、`platform-loadtest/run-k6.sh`：一键运行入口
- kind 本地集群实验场：
  - `platform-loadtest/kind/kind-config.yaml`：3 节点集群
  - `platform-loadtest/kind/lab.ps1`、`platform-loadtest/kind/lab.sh`：构建 sample app 镜像、load 到 kind、用共享 Helm chart 部署 N 副本
- 为压测场景补充后端接口能力：
  - `platform-sample-app` 的 `/demo/lock` 支持可选 `holdMillis`（并做上限保护），并同步更新 OpenAPI 合同与前端生成类型
- 文档补充：`docs/getting-started.md` 增加并发/集群验证入口说明

## 2026-02-09 - 统一日志查看：本地 Loki + Promtail（可选）

Q（落地动作）:
- 为本地调试补齐“统一日志管理查看”的最小闭环，与 metrics/traces 栈一致，且不影响默认 PR 验证。

A（本次已落地）:
- `platform-deploy/compose/docker-compose.observability.yml` 增加 Loki + Promtail：
  - Loki：存储/查询日志（Grafana Explore）
  - Promtail：抓取 Docker 容器日志并推送到 Loki（通过 docker socket）
- Grafana provisioning 增加 Loki datasource：`platform-deploy/compose/observability/grafana/provisioning/datasources/datasources.yml`
- Promtail 配置：`platform-deploy/compose/observability/promtail.yml`（基础标签：container/compose_service/compose_project/stream）
- 文档更新：`platform-deploy/README.md`、`docs/getting-started.md`（说明：host 方式运行的应用日志不会自动进 Loki，需用 Docker/Compose 运行）

## 2026-02-09 - 依赖治理：三年规则审计脚本（opt-in）

Q（落地动作）:
- 增加可执行的依赖年龄门禁：默认不影响 PR fast path，但发布前/CI 可一键检查；超过 3 年的依赖要求 vendor 到本地 repo 便于自维护。

A（本次已落地）:
- 新增依赖年龄审计脚本：
  - `scripts/deps-age-audit.py`：通过 Maven Central 上 POM 的 `Last-Modified` 估算依赖发布年龄，超龄则要求 vendored
  - `scripts/deps-age-audit.ps1`、`scripts/deps-age-audit.sh`：一键入口
  - 默认检查 direct runtime deps；可用 `--include-transitive` 扩展到全量传递依赖
  - 可用 `--vendor` 自动从本地 `~/.m2` 复制 jar+pom 到 `platform-thirdparty-repo/`（Maven file repo 结构）
- 新增例外清单模板：`platform-deps-metadata/age-exceptions.json`
- 文档更新：`platform-deps-metadata/README.md`、`CONTRIBUTING.md`
- Nightly CI（非 PR fast path）增加依赖年龄审计：`.github/workflows/nightly.yml`

## 2026-02-09 - Helm 部署修正：资源命名与 release 对齐

Q（落地动作）:
- 修正 Helm chart 资源命名策略，使“一键 Helm 部署/回滚/rollout gate”按 release 名称直观工作（deployment/svc 名称可预测），并避免同命名冲突。

A（本次已落地）:
- Helm chart 默认使用 `.Release.Name` 作为资源名（deployment/service），并保留 `fullnameOverride` 以便特殊场景覆盖：
  - `platform-deploy/helm/platform-service/templates/_helpers.tpl`
- 文档补充：`platform-deploy/README.md` 说明 release 名与 k8s 资源名的映射关系

## 2026-02-09 - 发布前验证：release-verify 一键脚本（opt-in）

Q（落地动作）:
- 增加“一键发布前验证”入口：尽量复用现有最小验证链路，同时把可选的运行态验证（Docker 启动门禁、k6、Python 黑盒）串起来，便于发布前快速复核。

A（本次已落地）:
- 新增脚本：
  - `scripts/release-verify.ps1`：Level=fast/standard/full；standard 默认跑 `mvn -Pit verify` + 依赖年龄审计 +（可选）Docker/k6/Python
  - `scripts/release-verify.sh`：Bash 版本（同逻辑，面向 Linux/macOS）
- 脚本健壮性（避免“子脚本 exit 导致上层脚本提前退出”）：
  - `platform-deploy/verify-http.ps1` 改为 `return`（可被其它脚本安全调用）
  - `platform-deploy/deploy.ps1`、`platform-loadtest/run-k6.ps1`、`platform-loadtest/kind/lab.ps1`、`scripts/deps-age-audit.ps1` 对外部命令非 0 退出码 fail-fast
- 文档补充：
  - `docs/getting-started.md` 增加 “1.5 一键发布前验证”
  - `CONTRIBUTING.md` 增加 Pre-release verification 入口

## 2026-02-09 - 业务可插拔模块化编排：Flow 引擎 + 业务样例 + 前端看板 + Python 黑盒验证

Q（落地动作）:
- 业务快速解耦式接入：按需加载模块化（类似 skill），支持顺序/并发/汇流的业务编排
- 给出 3-4 个业务测试项目：采集度量（DAG）、CRUD 聚合、工作流、聚合看板/跟踪；支持重试与部分更新组合
- 系统自身接口监控测试：可快速跑黑盒验证

A（本次已落地）:
- Flow 基建（DAG 编排 + 运行追踪）:
  - `platform-spi-flow`：step/flow SPI（禁止依赖 Spring/Jakarta）
  - `platform-flow-core`：DAG 引擎 + in-memory run/artifacts store
  - `platform-autoconfigure-flow` + `platform-starter-flow`：Spring Boot 自动装配与 Web API
- Flow Web API：
  - `GET /flows`、`POST /flows/{flowId}/runs`、`GET /flows/{flowId}/runs/{runId}`、`POST /flows/{flowId}/runs/{runId}/retry`
  - 新增 `GET /flows/{flowId}/runs/{runId}/artifacts` 便于调试/看板展示
- 业务样例模块（可插拔 auto-configuration，按依赖“组合成一组”）：
  - 采集度量：`platform-sample-biz-metrics-collector` + `platform-sample-biz-metrics-measure` + `platform-sample-biz-metrics-flow`（flowId=`demo.metrics`）
  - 工作流：`platform-sample-biz-workflow`（flowId=`demo.workflow.release`，支持 `failStepId` 注入失败用于验证重试）
  - CRUD：`platform-sample-biz-crud`（`/crud/todos`）
- 统一合同/调试：
  - `platform-sample-app` 引入 starter-flow + 业务样例模块，并扩展 `openapi.yaml` 覆盖 flows/crud
  - `platform-frontend-vue` 重新生成 OpenAPI types，并新增 Flows/Todos 调试看板（启动/重试/查看 artifacts）
- Python 黑盒验证：
  - `platform-test-python` 增加 `test_flow.py`、`test_crud.py`，用于接口监控与快速回归
