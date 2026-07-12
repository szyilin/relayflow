# 任务：IM 平台基础架构（im-platform-foundation）

> **性质**：架构 / 规格 change，**不写业务代码**。实现见 design.md §纵向切片路线图。

## 1. 文档与规格

- [x] 1.1 审阅 `proposal.md`、`design.md` 与 `specs/` 增量，确认四层架构、Envelope、数据模型、API 契约与路线图一致
- [x] 1.2 确认「V1 实现 vs 占位接口」边界清晰（notify / presence / NotifyInboxApi 仅预留）
- [x] 1.3 运行 `openspec validate im-platform-foundation --strict`

## 2. 归档与衔接（文档完成后）

- [x] 2.1 运行 `openspec archive im-platform-foundation` 合并 spec delta 至 `openspec/specs/`
- [x] 2.2 更新 `AGENTS.md`「下一优先」：`im-realtime-platform` → `im-schema-v1` → `im-direct-chat-*`

## 3. 后续实现 change（不在本 change 勾选）

以下各自独立 OpenSpec change，**须引用本 design**：

| 顺序 | Change | 说明 |
|------|--------|------|
| 1 | `im-realtime-platform` | `[平台]` starter-websocket + RealtimeTransportApi + DomainRouter |
| 2 | `im-schema-v1` | `[平台]` Flyway `im_*` + codegen |
| 3 | `im-direct-chat-web` | UI + Mock + `openspec/lanes/im-direct-chat/contract.md` |
| 4 | `im-direct-chat-api` | 单聊 REST + WS 业务 |
| 5 | `im-direct-chat-integrate` | 去 Mock 联调 |

验证（本 change 唯一命令）：

```bash
openspec validate im-platform-foundation --strict
```
