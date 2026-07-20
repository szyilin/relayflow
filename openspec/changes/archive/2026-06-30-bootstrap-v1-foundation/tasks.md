# 任务：bootstrap-v1-foundation

## 1. 文档与规格

- [x] 1.1 编写 proposal.md、design.md、tasks.md
- [x] 1.2 编写 deployment / system / infra / im 域 delta spec
- [x] 1.3 更新 openspec/config.yaml、README.md、AGENTS.md
- [x] 1.4 运行 `openspec validate bootstrap-v1-foundation --strict`
- [x] 1.5 运行 `openspec archive bootstrap-v1-foundation` 合并规格到 openspec/specs/

## 2. Maven 骨架（后续 change）

- [x] 2.1 创建父 pom.xml 与 relayflow-dependencies BOM — **关闭（路线重置，不再作为当前 backlog）**
- [x] 2.2 创建 relayflow-framework 各 starter 空模块 — **关闭（路线重置，不再作为当前 backlog）**
- [x] 2.3 创建 relayflow-module-system（api + biz） — **关闭（路线重置，不再作为当前 backlog）**
- [x] 2.4 创建 relayflow-module-infra（api + biz） — **关闭（路线重置，不再作为当前 backlog）**
- [x] 2.5 创建 relayflow-module-im（api + biz） — **关闭（路线重置，不再作为当前 backlog）**
- [x] 2.6 创建 relayflow-server 启动模块 — **关闭（路线重置，不再作为当前 backlog）**

## 3. 部署（后续 change）

- [x] 3.1 编写 deploy/compose.yml（PostgreSQL、Redis、MinIO） — **关闭（路线重置，不再作为当前 backlog）**
- [x] 3.2 编写 db/ 初始化脚本 — **关闭（路线重置，不再作为当前 backlog）**
- [x] 3.3 编写 .env.example — **关闭（路线重置，不再作为当前 backlog）**

## 4. 前端（后续 change）

- [x] 4.1 在 web/ 初始化 Vite + Vue 3 + TypeScript — **关闭（路线重置，不再作为当前 backlog）**
