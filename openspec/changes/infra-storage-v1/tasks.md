# 任务：infra-storage-v1

> **性质**：规划型史诗 — **本 change 不写业务代码**。勾选表示子 change 文档/实施完成。  
> **实施顺序**：按 §2 编号依次 propose → apply；单次会话只做一个子 change。

## 1. 史诗文档

- [x] 1.1 审阅 `proposal.md`、`design.md`、`specs/infra/spec.md` 增量
- [x] 1.2 运行 `openspec validate infra-storage-v1 --strict`

## 2. 子 change 立项（按顺序 propose，各 change ≤10 tasks）

> 每个子 change 须含 `proposal.md`、`design.md`、`tasks.md`、必要 spec delta；有 UI 的须 `openspec/lanes/{slice}/contract.md`。

- [x] 2.0 `infra-storage-platform` 已 propose（见 `openspec/changes/infra-storage-platform/`）
- [ ] 2.0b ②–⑧ 子 change 在实施前按需 `openspec new change <name>`

### ① `infra-storage-platform` `[平台]`

策略骨架 + MinIO 实现 + Bootstrap 启动校验 + `application.yml` 绑定。

| # | 任务摘要 |
|---|----------|
| 1 | BOM 引入 MinIO SDK |
| 2 | `ObjectStorageProviderType` + `ObjectStorageClient` 接口 |
| 3 | `MinioObjectStorageClient` 实现（connectivity / presign put/get / head / delete） |
| 4 | `ObjectStorageClientFactory`（V1 仅 MINIO 分支） |
| 5 | `StorageProperties` + `application.yml` / `.env.example` |
| 6 | `StorageBootstrapValidator` 启动校验 |
| 7 | `OssAutoConfiguration` 注册 Bean |
| 8 | `./mvnw -pl relayflow-server -am compile` |

### ② `infra-storage-schema` `[平台]`

Flyway + 权限种子 + codegen。

| # | 任务摘要 |
|---|----------|
| 1 | `V0.1.0.5__init_infra_storage.sql`（`infra_storage_provider`） |
| 2 | 同脚本或 `V0.1.0.6`：`infra_file`、`infra_file_upload_session`、`infra_file_binding` |
| 3 | 种子 `infra:*` 权限点 + `super_admin` 绑定 |
| 4 | `./scripts/codegen.sh --module infra --tables …` |
| 5 | 合并 DO/Mapper 至 `infra-biz` |
| 6 | `infra-biz` pom 引入 web/security/mybatis/tenant/oss starters |
| 7 | `./mvnw -pl relayflow-module-infra/relayflow-module-infra-biz -am compile` |

### ③ `infra-storage-config-api`

租户存储配置后端。

| # | 任务摘要 |
|---|----------|
| 1 | `StorageProviderService` CRUD + 密钥加解密 |
| 2 | `GET/PUT /admin-api/infra/storage/config` |
| 3 | `POST /admin-api/infra/storage/test-connection` |
| 4 | 删除前校验 `infra_file` 引用数 |
| 5 | `@PreAuthorize` + RBAC |
| 6 | curl 验收 + `mvn compile` |

### ④ `infra-storage-config-web`

存储设置管理页（前端优先）。

| # | 任务摘要 |
|---|----------|
| 1 | 起草 `openspec/lanes/infra-storage-config/contract.md` |
| 2 | `api/admin/storage.ts` + `stores/storage.ts` |
| 3 | 页面 `/admin/infra/storage`（表单 + 测试连接） |
| 4 | `useAdminNav` 增加「存储设置」 |
| 5 | `pnpm build`；浏览器 `/admin/infra/storage` |

### ⑤ `infra-file-upload-api`

直传 Session + Confirm。

| # | 任务摘要 |
|---|----------|
| 1 | 起草 `openspec/lanes/infra-file-upload/contract.md` |
| 2 | `FileUploadSessionService`（create + confirm + HeadObject 校验） |
| 3 | `FileService` + `infra_file` 持久化 |
| 4 | `POST upload-session` / `POST confirm` Controller |
| 5 | `FileApi`（`infra-api`）供跨域 `getFile` / `bindFile` |
| 6 | curl 直传 MinIO 验收 + `mvn compile` |

### ⑥ `infra-file-web`

文件管理页 + 前端直传。

| # | 任务摘要 |
|---|----------|
| 1 | 起草/完善 `openspec/lanes/infra-file/contract.md`（含列表 API） |
| 2 | `composables/useDirectUpload.ts` |
| 3 | `api/admin/file.ts` + `stores/file.ts` |
| 4 | 改造 `/admin/infra/file`（列表 + 上传 + 删除） |
| 5 | `pnpm build`；浏览器直传验证 |

### ⑦ `infra-file-download-api`

下载分流。

| # | 任务摘要 |
|---|----------|
| 1 | `GET /app-api/infra/file/public/{fileId}` |
| 2 | `GET /admin-api/infra/file/{fileId}/download`（私有 + RBAC） |
| 3 | presigned GET 302 + Cache-Control 策略 |
| 4 | curl / 浏览器验收 |

### ⑧ `infra-file-integrate`

联调与收尾。

| # | 任务摘要 |
|---|----------|
| 1 | store 去除 Mock；`spring-boot:run` + `pnpm dev` 全链路 |
| 2 | 更新 `docs/dev/api-integration-board.md` |
| 3 | `openspec validate` 各子 change |
| 4 | archive ③④⑤⑥⑦ 子 change |
| 5 | 勾选 `tenant-ready-foundation` §5.4（MinIO 前缀） |

## 3. 史诗归档（全部子 change 实施后）

- [ ] 3.1 更新 `AGENTS.md`「下一优先」
- [ ] 3.2 `openspec archive infra-storage-v1` 合并 spec 至 `openspec/specs/infra/spec.md`

## 不在本 change

- Java / `web/` 实现（→ 各子 change ①–⑧）
- OSS/COS 第二家厂商实现
- IM/头像等业务域对接 `fileId`（后续各业务切片）

## 建议 AI 会话粒度

```text
会话 A：openspec propose + apply ① infra-storage-platform
会话 B：openspec propose + apply ② infra-storage-schema
会话 C：propose ③④ → apply ③ → apply ④
会话 D：propose ⑤⑥ → apply ⑤ → apply ⑥
会话 E：propose ⑦ → apply ⑦
会话 F：propose ⑧ → apply ⑧ → archive 史诗
```

每会话仅一个子 change 的 `tasks.md` 未勾选项，降低出错率。
