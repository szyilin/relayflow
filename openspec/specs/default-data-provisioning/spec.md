# default-data-provisioning Specification

## Purpose

默认数据初始化惯例：按 A/B/C 分类选型，禁止跨域上帝填充器，惯例文档可发现。

## Requirements

### Requirement: 默认数据按 A/B/C 分类选型

系统与后续实现 MUST 将「跟租户 / 跟成员 / 跟实体走」的默认数据归入下列三类之一，并在设计文档或 tasks 中标明类别：

- **A 必须物化**：无持久行则功能不可用；生命周期事件上幂等 ensure。
- **B 默认即无行**：默认行为由代码或策略表达；不得仅为「记下默认」而预插行。
- **C 偏好文档**：读取时与代码默认合并；仅在用户（或管理员）显式变更时 upsert。

#### Scenario: 新偏好类能力选型

- **WHEN** 新增企业内用户设置/偏好类能力
- **THEN** 设计 MUST 归为 C 类（读合并 + 写 upsert）
- **AND** MUST NOT 在成员激活时批量插入完整默认偏好行

#### Scenario: 组织骨架类能力选型

- **WHEN** 新增依赖组织树或必须存在的租户级骨架数据
- **THEN** 设计 MUST 归为 A 类并提供幂等 ensure
- **AND** 可在成员激活或租户创建路径调用

### Requirement: 禁止跨域上帝填充器

实现 MUST NOT 引入单一服务直接写入多个域表前缀（`sys_` / `im_` / `infra_` 等）以完成「默认数据初始化」。各域 MUST 在本域内实现 ensure；跨域副作用 MUST 经 `*-api` 或领域消息。

#### Scenario: 成员激活派发

- **WHEN** 成员在租户内变为可登录/激活
- **THEN** 系统 MAY 发布生命周期事件供各域 A 类监听器处理
- **AND** 监听器 MUST 只修改本域表或通过正式 API 协作
- **AND** MUST NOT 由单一 Bean 直接 Mapper 访问他域表

### Requirement: 惯例文档可发现

仓库 MUST 提供 `docs/dev/default-data-provisioning.md` 描述 A/B/C 与禁止项，并在架构或 AGENTS 文档地图中可发现。

#### Scenario: 开发者查阅

- **WHEN** 实现者需要决定默认数据是否预插
- **THEN** 可从 `docs/dev/default-data-provisioning.md` 获得 A/B/C 定义与示例
