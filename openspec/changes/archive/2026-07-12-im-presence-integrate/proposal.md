# 提案：在线状态联调（im-presence-integrate）

## Why

`im-presence-web` 与 `im-presence-api` 完成后，去除 Mock，验证双用户在线/离线展示与轮询刷新。

## What Changes

- store 去 Mock
- 消息页 + 通讯录 Aside 联调
- 看板 `im-presence` → done

## 前置

- `im-presence-api` ready
