# 提案：管理端壳层 · 集成 Lane（admin-shell-integrate）

## 背景

`admin-shell-api` 与 `admin-shell-web` 并行完成后，本 change 负责 **联调、跨端小修、双端 validate**，作为该切片的交付门禁。

## 前置条件

- [ ] `admin-shell-api` 全部 tasks 完成
- [ ] `admin-shell-web` 全部 tasks 完成

## 范围

- 本地 `spring-boot:run` + `pnpm dev` 浏览器验收
- 必要时 **最小** 跨端修复（proxy、env、CORS — 应已在 -api/-web 完成）
- 三个 change 的 `openspec validate --strict`

## 非目标

- 新功能开发
- 大规模 refactor

## 用户可见结果

登录 `admin` / `admin123` → `/admin` navbar 显示数据库中的租户名 → 退出回登录页。
