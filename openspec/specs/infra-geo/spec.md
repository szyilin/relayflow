# infra-geo Specification

## Purpose

平台级中国行政区划存储、同步与级联查询能力。

## Requirements

### Requirement: 中国行政区划平台存储

系统 SHALL 以平台级表维护中国行政区划层级数据（非租户隔离），供管理端地址级联选择使用。V1 MUST 覆盖中国大陆至区县四级；MUST NOT 要求实现全球行政区或乡镇街道。

#### Scenario: 四级结构

- **WHEN** 同步或 bootstrap 完成后查询根节点下中国大陆路径
- **THEN** 可依次展开到省、市、区县
- **AND** 每条记录含稳定 `code`、显示名、层级与父节点关系

### Requirement: 行政区划来源可追溯

导入与同步 MUST 记录数据来源标识与版本（或同步时间），数据来自公开行政区划数据集或主管部门公开接口的可复现产物，而非人工逐条维护全国数据。

#### Scenario: 同步元数据

- **WHEN** 一次中国区划同步成功
- **THEN** 系统可查询到本次 `source`、`data_version`（或等价）与成功时间

### Requirement: 月度同步 Job

系统 SHALL 提供名为约定 handler（如 `infraGeoSyncChina`）的 XXL Job，默认按月执行，对区划数据做幂等 upsert。

#### Scenario: 重复执行幂等

- **WHEN** 同一数据版本的同步 Job 被执行两次
- **THEN** 区划主数据不产生重复业务键冲突
- **AND** 已有 `code` 记录被更新而非插入第二条有效记录

#### Scenario: 空库 bootstrap

- **WHEN** 平台区划表为空且执行同步或显式 bootstrap
- **THEN** 表被填充为可用的中国大陆四级数据，足以支撑级联选择器

### Requirement: 级联子节点查询 API

系统 SHALL 暴露管理端 API，按父节点懒加载子行政区，供企业地址表单使用。跨域业务模块若需只读区划，MUST 经 `infra-api` 契约，禁止他域 Mapper 直查 geo 表。

#### Scenario: 懒加载子节点

- **WHEN** 已认证且具备相应权限的管理员请求某 `parentId` 的子节点（根用约定空/0）
- **THEN** 返回直接子级列表（id、code、name、level、是否有子节点等）
- **AND** 不强制一次返回全国全量树
