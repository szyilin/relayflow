## 1. 设置窗壳层

- [x] 1.1 将名片「设置」改为打开独立设置窗（宽模态）；左栏分类 + 右栏内容；默认「通用」
- [x] 1.2 左栏：账号与安全、通用、隐私、通知等；非通用为显式占位

## 2. 通用三项

- [x] 2.1 主题模式：跟随系统 / 浅色 / 深色，写入 color mode；可附简易预览
- [x] 2.2 主题色色点选择，写入 app primary（或约定 token）
- [x] 2.3 会话显示模式：左对齐 / 左右分布；消息页读取并应用布局 class

## 3. Store 与契约

- [x] 3.1 `stores/userPreference`（或等价）持默认常量与本地状态；页面不直接 mock import
- [x] 3.2 起草 `openspec/lanes/user-preference/contract.md`（字段与后续 GET/PUT 对齐）
- [x] 3.3 更新 `docs/dev/workspace-ui-patterns.md` 设置窗说明

## 4. 验证

- [x] 4.1 `cd web && pnpm build && pnpm typecheck`
- [x] 4.2 浏览器：名片 → 设置 → 切换主题/气泡布局可见效果
