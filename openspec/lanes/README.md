# 并行 Lane · 切片契约（永久目录）

本目录存放 **前后端并行切片** 的 API 契约，**不随 `-api` change 归档而删除**。

| 文件 | 职责 |
|------|------|
| `{slice}/contract.md` | 路径、字段、鉴权、curl 示例；`-web` lane 对接依据 |
| [`docs/dev/api-integration-board.md`](../../docs/dev/api-integration-board.md) | **对接看板**：API/Web 是否已就绪、前端待建文件 |

## 生命周期

```text
T0  -api 起草 contract.md 并冻结
T1  -api / -web 并行实现
T2  -api 验收通过 → 更新看板 api=archived → archive -api change
T3  -web 读看板 + contract → 对接
T4  -integrate 联调 → archive -web、-integrate
```

契约变更：**仅** `-api` lane 或人工修改 `contract.md`，并同步更新看板与通知 `-web`。

```text
openspec/lanes/
  admin-shell/
    contract.md
  admin-user-list/   （待建）
    contract.md
```

说明：[`docs/dev/parallel-lane-workflow.md`](../../docs/dev/parallel-lane-workflow.md)
