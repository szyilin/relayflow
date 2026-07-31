# 工作台插画资源（Storyset）

> **给 AI / 实现者**：工作台需要**插画**（空态大图、引导示意、建设中示意等）时，**统一从 Storyset 取图**，在官网调色与裁层后再落仓，禁止从 UGC 图标站零散拼风格。  
> 界面**控件图标**仍用 Lucide（Nuxt UI），与本文无关。

## 真源与入口

| 项 | 约定 |
|----|------|
| 官网 | [https://storyset.com/](https://storyset.com/) |
| How it works | [https://storyset.com/how-it-works](https://storyset.com/how-it-works) |
| FAQ | [https://storyset.com/faqs](https://storyset.com/faqs) |
| 本地落盘 | `web/src/assets/illustrations/`（按语义命名，见下） |
| 接入组件 | 优先 `WorkspaceEmptyState` 的 `#illustration` slot；勿在业务页硬编码外链图 |

## AI 工作流（必须按此顺序）

1. **判断是否需要插画**  
   空列表、无选中主区、设置占位、「即将推出」、引导/欢迎示意等需要视觉锚点时 → 用 Storyset。  
   按钮、导航、列表行小图标 → 仍用 Lucide，**不要**用 Storyset。

2. **在 Storyset 搜索概念**  
   用英文关键词搜场景（如 `empty`、`search`、`team`、`documents`、`tasks`、`security`、`under construction`）。  
   选图时对齐产品语义，不要为「好看」换无关题材。

3. **固定一种画风（全产品统一）**  
   同一产品面（员工工作台）内只选 **一种** Storyset style，后续空态一律沿用。推荐默认：**Rafiki**（干净 flat，适合协作产品）。若仓库里已有落盘文件，**以已有文件的 style 为准**，禁止混用 Bro / Amico / Pana / Cuate。

4. **在官网改完再下载（优先路径）**  
   Storyset 在线编辑能力很全；**色、背景、图层、动效优先在官网调好**，再导出。  
   禁止：随便下未调色的图 → 在仓库里用脚本乱改一堆 `fill` 当「主题对齐」。  
   允许：落仓后的 SVG 做**小幅**适配（尺寸、去掉多余 wrapper、配合现有 CSS）；大改色/换题材仍回官网。

5. **下载格式**  
   - 静态空态 / 示意：**优先 SVG**（可缩放、易进 Git）  
   - 仅当 SVG 表现异常时用 PNG（注意分辨率）  
   - 动效：官网可出 HTML（内联 SVG+CSS）/ GIF / MP4；工作台空态默认**静态**，动效需产品明确要求后再用

6. **保存到前端项目**  
   - 路径：`web/src/assets/illustrations/<语义名>.svg`  
   - 命名：小写 kebab-case，表达场景，例如：  
     - `empty-inbox.svg`  
     - `messages-idle-chatting.svg`（消息主区未选会话池）  
     - `coming-soon.svg`  
   - **目录登记**：新增文件后 MUST 写入 `web/src/utils/workspaceIllustrations.ts` 对应池，禁止页面散落 `import` 一堆裸路径  
   - 同语义只保留一份；替换图时覆盖同名文件并更新引用  
   - **禁止**把未使用的整包插画库塞进仓库；按需一图一文件

### 消息主区未选会话（idle）

| 项 | 约定 |
|----|------|
| 何时展示 | 有会话列表但**当前未选中**任一会话（无 `conversationId` 深链、无会话内残留选中、非 pending 单聊） |
| 插画 | 从 `MESSAGES_IDLE_ILLUSTRATIONS` **每次进入未选态随机抽一张**（单次访问内固定，勿每帧重抽） |
| 文案 | 插画 `caption` 作主标题 + 一句「从左侧列表选择会话…」 |
| 选中恢复 | 同 SPA 会话内曾选中 → 再进消息页可恢复；URL `?conversationId=` 可恢复 |
| **禁止** | 进入消息页时自动选中列表第一条（会触发已读上报，用户未点开的未读会被当成已读） |

7. **许可与署名**  
   Storyset 免费档要求注明来源（见 [FAQ · attribution](https://storyset.com/faqs)）。  
   在落盘目录或组件旁用简短注释 / README 片段标明来源即可（例如文件头注释或 `illustrations/README.md` 一行：`Illustration from Storyset (storyset.com)`）。  
   具体条款以官网当期为准；若项目后续改用需付费/免署名方案，再单独改本文。

## 官网可调项（AI 找图/改图时对照）

进入某张插画后，主要在页面右侧 / 顶栏操作（文案以官网 UI 为准）：

| 能力 | 做什么 | 工作台建议 |
|------|--------|------------|
| **Style** | Rafiki / Bro / Amico / Pana / Cuate | 全产品锁一种；默认 Rafiki |
| **Change color** | 预设色、取色器或自填 Hex/RGB | 主色对齐工作台主题色（`--ui-primary` / 当前品牌色）；避免高饱和彩虹 |
| **Background** | Hidden / Simple / Detailed（部分 style 无全部选项） | 空态优先 **Hidden**，少抢壳层与卡片 |
| **Layers** | 右侧图层列表，点眼睛隐藏元素 | 去掉多余道具/文字感装饰，只留语义主体 |
| **Animate it** | Entrance + Looping；可调 duration / delay / easing；可 Randomize / Reset | 空态默认不做；营销页另议 |
| **Download** | SVG / PNG；动效 HTML / GIF / MP4 | 空态用 SVG |

更多逐步说明见官网 [How it works](https://storyset.com/how-it-works) 与 [FAQs](https://storyset.com/faqs)。

## 与现有组件的关系

| 层 | 做法 |
|----|------|
| 容器 | `WorkspaceEmptyState`：标题 + 描述 + `#illustration` + `#actions` |
| 无插画时 | slot 默认仍可用 Lucide `icon`（小图标占位）；**有正式空态语义时优先换 Storyset SVG** |
| 样式 | 插画外圈仍可用 `.workspace-empty-illust`；SVG 本身勿再加厚描边、霓虹阴影 |

## 禁止

- 从 iconfont / 随机 SVG 站 / 截图拼一套「假 Storyset」风格  
- 同一工作台混用多种 Storyset style  
- 插画外链热更新（必须落本地，保证自部署可离线）  
- 用插画代替状态色（官方/机器人/外部等仍走 `WorkspaceSemanticBadge` / 头像体系）  
- 在规格或用户可见文案里堆砌无关第三方品牌营销话术（本文件为内部工程约定，可写 Storyset 名）

## 参考

- [workspace-ui-patterns.md](workspace-ui-patterns.md) — 空态组件与页面模式  
- [workspace-ui-tokens.md](workspace-ui-tokens.md) — 色用在人/态/空，画布保持冷静  
- Storyset：[storyset.com](https://storyset.com/)
