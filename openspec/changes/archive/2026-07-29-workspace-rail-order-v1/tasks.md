## 1. Preference schema (backend + frontend types)

- [x] 1.1 `UserPreferenceDefaults`：codeDefaults 增加 `workspace.railOrder` 默认数组；sanitize 接受/清洗 string[]
- [x] 1.2 前端 `api/app/userPreference` + `stores/userPreference`：类型、默认值、deepMerge、`setRailOrder`
- [x] 1.3 更新 `openspec/lanes/user-preference/contract.md` 文档键说明

## 2. Rail order resolve + UI

- [x] 2.1 抽出 `DEFAULT_RAIL_ORDER` + `resolveRailOrder`；`useWorkspaceNav` 按偏好排序
- [x] 2.2 `WorkspaceRail.vue`：HTML5 拖拽重排 + 插入线落点；避免误导航
- [x] 2.3 drop 后调用 `setRailOrder`；失败 toast

## 3. Verify

- [x] 3.1 `./mvnw -pl relayflow-server -am compile`
- [x] 3.2 `cd web && pnpm build && pnpm typecheck`
- [x] 3.3 浏览器冒烟说明：登录 → 拖拽侧栏 → 刷新仍保持；未自定义用户见默认序
