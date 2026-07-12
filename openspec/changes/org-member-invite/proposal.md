# 提案：组织成员邀请（org-member-invite）

## 背景

当前管理端「新建用户」流程由组织直接创建账号并设置密码，与飞书式产品模型不符：

- **账号属于个人**：注册、密码由用户自己管理；一账号可加入多家企业
- **组织只能邀请**：通过手机号（或邮箱）邀请已有/未来账号加入本租户
- **组织内属性**：姓名、工作邮箱、部门、角色、头像等归属当前企业
- **成员状态**：`正常`（ACTIVE）、`待同意`（NOT_JOINED）等，挂在 `sys_tenant_user.status`

架构真源已定义成员生命周期枚举（`system-schema-v1` design D5），但 `user/create` 仍直接创建 ACTIVE 账号并强制密码。

## 目标

1. 管理端将「新建用户」改为「邀请成员」语义与 UI（参考飞书添加成员）
2. 新增 `POST /admin-api/system/user/invite`：按手机号邀请，成员状态 `NOT_JOINED`
3. 列表展示「账号状态」：正常 / 待同意 / 已暂停（V1 不做自主申请加入）
4. 表单布局加宽，双栏分组，去掉用户名与密码字段

## 非目标

- 通知中心、站内信展示邀请（后续 change）
- 用户端「同意邀请」API 与 UI（后续 `org-member-invite-accept`）
- 自主申请加入组织
- 租户内昵称/邮箱与全局 `sys_user` 字段拆分（Phase 2 数据模型演进）
- 废弃 bootstrap 用的 `user/create`（保留给种子管理员）

## 影响

| 层 | 变更 |
|----|------|
| API | 新增 `invite`；`user/page` 响应增加 `memberStatus`、`mobile` |
| 前端 | `/admin/system/user/create` 改文案与表单；列表列与状态徽章 |
| DB | 无迁移（复用 `NOT_JOINED`） |
