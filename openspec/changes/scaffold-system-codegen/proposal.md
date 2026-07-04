# 提案：CLI 按需代码生成（scaffold-system-codegen）

## Why

DO/Mapper 须从数据库元数据生成，但 **不应** 在 Java 代码中硬编码表清单，也不应在 `mvn compile` 时全库自动生成。开发者与 AI 需要：**指定表 → 输出到临时目录 → diff → 再合并到模块**。

## What Changes

- `relayflow-tools/relayflow-codegen`：MyBatis-Plus Generator **CLI**
- `scripts/codegen.sh`：命令行入口（人类与 AI 共用）
- `codegen.yml`：模块包名、枚举列映射（**非表清单**）
- 移除 `system-biz` 上 `generate-sources` 自动挂钩
- 更新 `docs/dev/codegen.md` 工作流

## Non-Goals

- 管理端 UI 选表生成（Phase 2 / infra 域 change）
- 自动 merge 进 `src/`（仅生成到临时目录，合并由人/AI diff 后执行）
- CI 全库 codegen 门禁（后续 change）
