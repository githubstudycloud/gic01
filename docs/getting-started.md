# Getting Started / 新手引导

目标：让第一次进入仓库的人（包括 AI）在 10 分钟内跑起来、看懂结构、知道怎么验证与扩展。

## 0. 你需要准备什么

- JDK 17+（推荐本地/生产运行 JDK 21）
- Maven 3.9+
- 可选：Docker（用于 adapter 集成测试 / 本地依赖栈）

## 1. 一键验证（最小化）

```bash
# 只跑单测（默认最小验证链路）
mvn -q test
```

如果需要跑 adapter 集成测试（可选）：

```bash
mvn -q -Pit verify
```

如果你希望编译目标为 Java 21（可选，需要 JDK 21+）：

```bash
mvn -q -Pjava21 test
```

## 1.5 一键“发布前验证”（可选）

将构建 + 依赖政策 +（可选）Docker 启动检查 +（可选）k6/Python 黑盒验证串起来：

```powershell
./scripts/release-verify.ps1 -Level standard
```

## 2. 跑一个最小可运行样例

```bash
mvn -q -pl platform-sample-app spring-boot:run
```

然后访问：

- `GET http://localhost:8080/demo/ping`
- `GET http://localhost:8080/actuator/health/readiness`

## 3. 跑汇总服务（可选）

```bash
mvn -q -pl platform-observability-hub spring-boot:run
```

Hub 会根据配置拉取各服务的 actuator 输出并汇总（health/info）。
默认端口：`8081`。

## 4. 看懂仓库结构（只记住这几条）

- `platform-parent`：统一插件、门禁、profile（Java 17/21、IT 等）
- `platform-bom`：唯一第三方依赖版本入口（业务模块禁止写版本号）
- `platform-kernel`：纯 Java 内核（严禁 Spring/Jakarta）
- `platform-spi-*`：端口层（接口 + DTO）
- `platform-adapter-*`：SPI 的实现（可替换/可并存）
- `platform-autoconfigure-*`：Spring Boot 自动装配
- `platform-starter-*`：starter-first 依赖聚合与默认约定
- `platform-deploy`：一键多方式部署骨架
- `platform-loadtest`：并发/集群验证脚手架（k6 + kind 实验场）

## 5. 预发布命名空间替换

预发布阶段使用 `com.test.platform`；后续替换请用：

- `scripts/rename-namespace.ps1`
- 说明：`docs/namespace-renaming.md`

## 6. 协作约定（人 + AI）

- `AGENTS.md`：本仓库 AI 协作规则（含必须追加 `docs/QA.md` 存档）
- `CONTRIBUTING.md`：贡献指南（边界/验证/依赖政策）

## 7. 本地可观测（可选）

- 默认日志为结构化 JSON（便于采集/检索）；如需本地更“轻”，可设置 `platform.logging.enabled=false`。
- 可用 `platform-deploy/compose/docker-compose.observability.yml` 一键启动本地 metrics + tracing + logs 栈。

## 8. Vue 前端（可选）

后端先跑样例服务：

```bash
mvn -q -pl platform-sample-app spring-boot:run
```

再启动前端（默认走 Vite proxy，无需配置 CORS）：

```bash
cd platform-frontend-vue
npm install
npm run gen:api
npm run dev
```

打开：`http://localhost:5173`

## 9. Python 黑盒测试（可选）

先启动后端样例（同上），然后：

```bash
cd platform-test-python
python -m venv .venv
.venv\\Scripts\\activate
pip install -r requirements.txt
pytest
```

## 10. 并发/集群验证（可选）

- k6（轻量并发/压测）：`platform-loadtest/run-k6.ps1`、`platform-loadtest/run-k6.sh`
- kind（本地 K8s 实验场）：`platform-loadtest/kind/lab.ps1`、`platform-loadtest/kind/lab.sh`

详见：`platform-loadtest/README.md`
