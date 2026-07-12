# Tasks：im-deepening-v1（母 change · 执行路线图）

> **用法**：总路线图；实际编码按 **子 change** 分批执行。  
> **顺序**：默认 **前端优先**（`-web` → `-api` → `-integrate`）。

---

## 0. 规划基线

- [x] 0.1 子 change 目录与 proposal/design/tasks 已创建
- [x] 0.2 lanes contract：`im-message-file`、`im-read-receipt`、`im-presence`
- [x] 0.3 `openspec validate im-deepening-v1 --strict`（全部子 change validate 通过）

---

## 1. im-group-chat-*（已完成 · 待 archive）

- [x] 1.1 `im-group-chat-web` / `-api` / `-integrate` tasks 全部完成
- [x] 1.2 archive 三子 change
- [x] 1.3 看板 `im-group-chat` 保持 **done**

---

## 2. im-message-file-*（附件）

- [x] 2.1 `im-message-file-web` — UI + contract + pnpm build
- [x] 2.2 `im-message-file-api` — file/image send + downloadUrl + app 私有下载
- [x] 2.3 `im-message-file-integrate` — 去 Mock、单聊/群聊冒烟
- [x] 2.4 archive `-web` / `-api` / `-integrate`

---

## 3. im-read-receipt-*（已读 UI）

- [x] 3.1 `im-read-receipt-web` — 单聊 outgoing「已读」+ contract
- [x] 3.2 `im-read-receipt-api` — read-status + WS read.updated
- [x] 3.3 双浏览器联调
- [x] 3.4 archive `-web` / `-api`

---

## 4. im-presence-*（在线状态）

- [x] 4.1 `im-presence-web` — messages/contacts Aside
- [x] 4.2 `im-presence-api` — batch REST
- [x] 4.3 `im-presence-integrate` — 去 Mock
- [x] 4.4 archive 三子 change

---

## 5. 母 change 归档

- [x] 5.1 全部子 change archived
- [x] 5.2 `./mvnw verify`（如适用）+ `cd web && pnpm build`
- [ ] 5.3 `openspec archive im-deepening-v1`（spec 已手动合并至 `openspec/specs/im/spec.md`）

---

## 执行顺序速查

```text
Session A   im-message-file-api          ✅
Session B   im-message-file-integrate    ✅
Session C   im-read-receipt-web + -api   ✅
Session D   im-presence-web              ✅
Session E   im-presence-api + integrate  ✅
Session F   archive 全部子 change + im-deepening-v1
```

## 会话开场白模板

```text
Using change: im-message-file-api（im-deepening-v1 子切片 · 后端 lane）
Read: openspec/lanes/im-message-file/contract.md
Tasks: im-message-file-api/tasks.md
```
