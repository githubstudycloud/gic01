# Documentation Versioning / 文档版本管理

本仓库采用“代码版本驱动文档版本”的策略：文档是产品的一部分，跟随代码发布、可回溯、可审计。

## 原则

- `main/master` 分支的 `docs/` 表示“最新（未发布/即将发布）的文档”。
- 每次发布（tag）都必须生成一份“可回溯”的发布说明（release notes）。
- 重要架构决策必须用 ADR（Architecture Decision Record）记录，避免口头/聊天丢失。
- 文档变更与代码变更尽量同 PR（让审阅者能理解“为什么这样改”）。

## 落地结构

- `docs/`：持续演进的“最新文档”
- `docs/adr/`：架构决策记录（按编号递增）
- `docs/releases/`：每个版本一份发布说明（`vX.Y.Z.md`）
- `CHANGELOG.md`：面向使用者的变更摘要（Keep a Changelog 风格）

## 发布时的文档清单（必须）

当准备发布 `vX.Y.Z`（打 tag）时：

- 新增 `docs/releases/vX.Y.Z.md`：
  - 关键特性/修复
  - 升级注意事项（Breaking changes）
  - 迁移指南（如需要）
  - 依赖/平台基线变化（JDK/Spring Boot/中间件）
- 更新 `CHANGELOG.md`：
  - 将 `Unreleased` 的内容整理进入 `vX.Y.Z`
- 如果本次有重要决策：
  - 新增 ADR（`docs/adr/NNNN-*.md`）

## 版本化站点（可选）

如果你们希望提供一个带版本切换的文档站点（例如 MkDocs/Docusaurus），建议作为后续增强：

- 先保证“结构与内容可回溯”（本文件定义的结构），再引入站点工具。
- 站点工具引入后，仍以 `docs/` 为单一事实来源（SSOT）。

