## ADDED Requirements

### Requirement: 受管对象存储内部与浏览器地址

系统必须（MUST）支持为受管对象存储配置内部 endpoint、可选浏览器 public endpoint 与 region。服务端连通校验 / HEAD / 删除必须使用内部地址，预签名 PUT / GET 必须使用公开地址参与签名；未配置 public endpoint 时必须保持原有单地址行为。

#### Scenario: 默认对象存储双地址

- **GIVEN** Bootstrap 使用容器内部地址与浏览器公开地址，且存储桶已初始化
- **WHEN** 后端创建上传会话、确认上传或生成下载链接
- **THEN** 浏览器可直接使用返回的原始签名 URL 上传 / 下载，后端通过内部地址完成校验
- **AND** 签名过程不依赖容器访问浏览器的 localhost，region 与受管存储一致

#### Scenario: 兼容单地址部署

- **GIVEN** 现有部署未设置 public endpoint
- **WHEN** 创建签名或执行对象操作
- **THEN** 继续使用已配置 endpoint，无需修改旧租户配置或迁移数据

#### Scenario: 双地址配置缺少 region

- **GIVEN** 已设置 public endpoint，但未设置受管存储 region
- **WHEN** 应用校验部署配置
- **THEN** 明确拒绝不完整配置，不通过访问浏览器地址进行 region 自动发现

### Requirement: 受管地址映射尊重租户存储

受管双地址解析必须（MUST）一致覆盖默认上传、租户上传、历史文件下载和 inline 连通测试，但仅作用于匹配受管内部 / 公开 endpoint 的配置。必须保留租户的凭证、bucket 与路径前缀，不得改写独立存储配置或回写其持久 JSON。

#### Scenario: 匹配受管地址的租户配置

- **GIVEN** 租户配置的 endpoint 与受管内部或公开地址规范化后精确匹配
- **WHEN** 解析有效上传配置、历史下载配置或执行 inline 连接测试
- **THEN** 对象操作使用内部地址，签名使用公开地址，同时保留该租户配置的凭证、bucket 和路径前缀

#### Scenario: 独立对象存储

- **GIVEN** 租户配置指向另一套存储 endpoint
- **WHEN** 上传、读取历史文件或测试连接
- **THEN** 使用该租户原配置，不将其重定向至默认本地 MinIO

#### Scenario: 保持签名与访问控制

- **WHEN** 配置公开访问地址及本地浏览器跨域直传
- **THEN** 在 SDK 签名前选择公开地址，不在生成后替换 hostname / path，不通过匿名桶策略绕过签名与原有访问级别
