# 并行 Lane · 切片契约（永久目录）

本目录存放 **前后端切片** 的 API 契约，**不随 `-api` change 归档而删除**。

| 文件 | 职责 |
|------|------|
| `{slice}/contract.md` | 路径、字段、鉴权、curl；**`-web` 起草**，`-api` 实现 |
| [`docs/dev/api-integration-board.md`](../../docs/dev/api-integration-board.md) | UI/API 进度 |

## 生命周期（前端优先）

```text
T0  -web 起草 contract.md + UI（Mock）
T1  -web ui_ready → pnpm build
T2  -api 读 contract → 实现 → archive
T3  -integrate 去 Mock → 看板 done
```

契约变更：改 `contract.md` 并同步看板；integrate 阶段发现不一致时回写 contract。

```text
openspec/lanes/
  admin-shell/
    contract.md
  admin-user-list/
    contract.md
```

说明：[`docs/dev/frontend-first-workflow.md`](../../docs/dev/frontend-first-workflow.md)
