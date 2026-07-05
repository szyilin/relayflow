# 并行 Lane 共享契约

本目录存放 **前后端并行 change** 的 API 契约，供 `{slice}-api` 与 `{slice}-web` 共同引用。

- 起草：通常由 **`{slice}-api`** lane 完成 T0 后写入
- 修改：契约变更须同步通知另一 lane；集成问题在 **`{slice}-integrate`** 处理
- 说明：见 [`docs/dev/parallel-lane-workflow.md`](../../../docs/dev/parallel-lane-workflow.md)

```text
_lanes/
  admin-shell/
    contract.md
  admin-user-list/   （待建）
    contract.md
```
