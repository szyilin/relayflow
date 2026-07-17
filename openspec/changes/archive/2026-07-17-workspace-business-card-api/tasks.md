## 1. 库表

- [x] 1.1 Flyway：`sys_user` 增 `signature`、`cover_file_id`；新建 `sys_contact_remark`
- [x] 1.2 更新 `SysUserDO`/Mapper.xml；新增 ContactRemark DO/Mapper

## 2. API

- [x] 2.1 扩展 profile GET/PUT（signature、coverFileId）；`GET /profile/{userId}` 租户内只读
- [x] 2.2 `GET/PUT /app-api/system/contact-remark/{targetUserId}`
- [x] 2.3 冻结 lane contract；去掉「本地暂存」表述

## 3. 前端联调

- [x] 3.1 profile/businessCard store 改 API；封面上传 public file；删除 localStorage 真源
- [x] 3.2 名片组件读服务端签名/封面/备注

## 4. 验证

- [x] 4.1 `./mvnw -pl relayflow-server -am compile`
- [x] 4.2 `cd web && pnpm build && pnpm typecheck`
- [x] 4.3 `openspec validate workspace-business-card-api --strict`
