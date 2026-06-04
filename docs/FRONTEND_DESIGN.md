# Under-Utils 前台设计方案

本文定义 Under-Utils 三套前台的统一设计方案：官网入口、使用文档和 API 文档。方案参考 Hutool 站群的信息组织方式，但不复刻其实现；重点吸收其“入口清晰、文档完整、API 可查”的优点，同时修正风格割裂、版本不一致、移动端挤压、广告干扰、emoji 图标和 iframe API 文档等问题。

## 设计目标

- 用同一套品牌、导航、色彩、字体、组件和版本信息承载三套前台。
- 让用户在 30 秒内判断 Under-Utils 是否适合自己的 Java 21 / Spring Boot 项目。
- 让用户能按模块快速找到安装方式、配置 key、失败语义、示例代码和 public API。
- 让官网、文档和 API 文档之间保持一致的路径、搜索体验和版本状态。
- 保持桌面端高信息密度，不做移动端适配，也不预留广告或赞助位。

## 非目标

- 不做手机端、平板端、响应式断点和移动抽屉导航。
- 不做广告位、赞助位、推广横幅和外部商业卡片。
- 不把 Under-Utils 包装成 Hutool、Apache Commons 或 Guava 的替代品。
- 不用 iframe 承载 API 文档。
- 不使用 emoji 作为结构图标；图标统一使用 SVG 图标库。
- 不在三个前台分别维护版本号、模块列表和安装片段。

## 产品定位

Under-Utils 是面向 Java 21 / Spring Boot 3.1.x 项目的工程模式工具包。它关注跨项目复用的基础设施能力，例如请求上下文传播、限流、防重复提交、Redis 分布式锁、缓存模板、OpenAPI 客户端治理、AI 模型基础调用、安全分页、审计填充和导入任务流程。

官网和文档需要明确表达两个边界：

- 推荐方向：封装复杂、重复、跨项目复用、具备明确失败语义和测试覆盖的工程问题。
- 不推荐方向：JDK、Spring、Hutool、Apache Commons、Guava 已成熟覆盖的小工具方法，以及单业务线一次性流程。

## 三套前台

| 前台 | 建议路径 | 核心任务 | 主要用户 |
|------|----------|----------|----------|
| 官网入口 | `/` | 说明项目定位、模块能力、安装入口、版本状态和上手路径 | 首次访问者、选型者 |
| 使用文档 | `/docs/` | 承载指南、模块文档、配置、示例、兼容性和发布说明 | 接入开发者、维护者 |
| API 文档 | `/api/` | 查询 public API、包、类、方法、弃用状态和版本差异 | 深度使用者、升级者 |

三套前台共享同一个顶栏、搜索框、版本切换、模块索引、页脚和设计 token。用户从官网进入文档或 API 时，不应感知为三个不同产品。

## 技术栈与构建方案

首版前台建议做成静态站，不引入后端运行时。仓库当前主体是 Maven 多模块 Java 项目，前台应作为独立 `site/` 工程接入构建链路，读取现有 README、`docs/*`、模块 README 和 Javadoc 产物生成页面。

### 推荐技术栈

| 层级 | 技术 | 用途 |
|------|------|------|
| 站点生成 | Astro + TypeScript | 统一生成官网、文档和 API 三套前台，构建后输出纯静态文件 |
| 文档内容 | Markdown / MDX | 承载 `README.md`、`docs/*`、`under-utils-*/README.md`，少量交互说明可用 MDX 组件 |
| API 数据 | JDK Doclet API + Maven Javadoc Plugin | 从 Java public API 生成结构化索引和原始 Javadoc 归档 |
| 搜索 | Pagefind 或本地静态搜索索引 | 全站搜索文档、模块、配置 key、类名和方法名 |
| 样式 | CSS Variables + scoped CSS | 落地统一 token，避免页面内散落 raw hex 和内联样式 |
| 代码高亮 | Shiki | Maven、YAML、Java、Shell、JSON 等代码块高亮 |
| 图标 | Lucide SVG | 搜索、复制、外链、过滤、状态、展开收起等结构图标 |
| 数据文件 | JSON | `version.json`、`modules.json`、`api-index.json`、`search-meta.json` |
| 部署产物 | 静态文件 | 可部署到 GitHub Pages、Nginx、对象存储或任意静态站平台 |

不建议使用 VuePress/VitePress 分别搭文档站再额外维护官网和 API 站。该做法容易重复 Hutool 当前三套前台割裂的问题。也不建议直接发布 Javadoc frames 页面作为默认 API 入口。

### 工程目录建议

```text
site/
  package.json
  astro.config.*
  src/
    layouts/
      SiteShell.*
      DocsLayout.*
      ApiLayout.*
    pages/
      index.*
      docs/
      api/
    components/
      TopNav.*
      SearchBox.*
      VersionBadge.*
      CodeBlock.*
      ModuleCard.*
      ApiIndex.*
    styles/
      tokens.css
      global.css
    data/
      version.json
      modules.json
      api-index.json
  scripts/
    sync-docs.*
    generate-version.*
    generate-api-index.*
```

`site/src/layouts/SiteShell.*` 是三套前台的统一外壳，顶栏、版本、搜索、页脚和设计 token 都从这里进入。官网、文档、API 只切换主体布局，不允许各自重新实现一套导航。

### 内容来源映射

| 内容 | 来源 | 生成目标 |
|------|------|----------|
| 项目定位、安装、模块表 | `README.md` | `/`、`/docs/quick-start/` |
| 模块说明 | `under-utils-*/README.md` | `/docs/modules/{module}/` |
| 兼容性 | `docs/COMPATIBILITY.md` | `/docs/compatibility/` |
| API 审计 | `docs/API_REVIEW.md` | `/docs/api-review/` |
| 工程成熟度 | `docs/ENGINEERING_MATURITY.md` | `/docs/engineering-maturity/` |
| 发布指南 | `docs/RELEASE.md` | `/docs/release/` |
| Release Notes | `docs/releases/*.md`、`CHANGELOG.md` | `/docs/releases/` |
| API 索引 | Maven Javadoc + Doclet 输出 | `/api/` |
| 版本号 | Maven reactor version + release metadata | `site/src/data/version.json` |

### 构建流程

1. Maven 读取当前 reactor 版本，生成 `version.json`。
2. Maven Javadoc Plugin 生成原始 Javadoc 归档。
3. 自定义 Doclet 或构建脚本生成 `api-index.json`，包含模块、包、类、方法、deprecated 状态和替代说明。
4. `site/scripts/sync-docs.*` 将 README 与 `docs/*` 映射成站点内容。
5. Astro 构建官网、文档和 API 静态页面。
6. Pagefind 或等价工具生成静态搜索索引。
7. CI 发布 `site/dist`，不发布中间脚本产物。

### 约束

- Node 版本、包管理器和依赖版本在真正创建 `site/` 工程时再锁定，不在设计文档中写死。
- 前台不依赖 Java 服务运行；所有数据在构建时生成。
- API 文档默认入口消费结构化 JSON，不直接 iframe 原始 Javadoc。
- 文档源文件仍以仓库现有 Markdown 为准，避免官网和 README 长期分叉。
- 若后续选择其他静态生成器，必须保留统一 shell、统一搜索、统一版本源和无 iframe API 入口这些约束。

## 统一信息架构

顶级导航固定为：

| 导航 | 目标 | 说明 |
|------|------|------|
| 首页 | `/` | 项目概览和安装入口 |
| 文档 | `/docs/` | 快速开始与模块指南 |
| 模块 | `/docs/modules/` | 按 `under-utils-*` 查看功能 |
| API | `/api/` | public API 查询 |
| 兼容性 | `/docs/compatibility/` | 版本语义、public API、破坏性变更 |
| 发布 | `/docs/release/` | Maven Central、release notes、验证命令 |
| GitHub | 仓库地址 | 外链，使用统一外链图标 |

全局搜索范围：

- 文档标题、正文、模块名、配置 key、注解名、类名、方法名。
- 搜索结果按类型分组：文档、模块、配置、API、发布说明。
- 无结果时显示建议词，例如 `RateLimit`、`CacheAsideTemplate`、`AiClient`、`SafePageQuery`。

版本信息来源必须单一化：

- 稳定版本显示为当前已发布版本，例如 `1.0.3`。
- 开发版本显示为当前 main 分支的 SNAPSHOT 版本，例如 `1.0.4-SNAPSHOT`。
- 版本值由发布流程生成的 `site/version.json` 或等价构建产物提供，官网、文档和 API 不允许手写不同版本。
- 安装代码块、API 标题、版本切换器和 Release Notes 均读取同一来源。

## 视觉风格

采用 `ui-ux-pro-max` 推荐的开发者文档基线：Minimalism & Swiss Style。风格关键词为清晰、克制、工程化、高对比、网格化、低装饰。

### 色彩

| Token | 值 | 用途 |
|-------|----|------|
| `--color-background` | `#F8FAFC` | 页面背景 |
| `--color-surface` | `#FFFFFF` | 顶栏、卡片、文档正文 |
| `--color-surface-muted` | `#F1F5F9` | 次级区域、代码块外壳 |
| `--color-foreground` | `#1E293B` | 主文本 |
| `--color-muted-foreground` | `#64748B` | 辅助文本 |
| `--color-primary` | `#475569` | 顶栏文本、次级按钮 |
| `--color-accent` | `#2563EB` | 主 CTA、链接、当前项 |
| `--color-success` | `#059669` | 稳定、通过、成功状态 |
| `--color-warning` | `#D97706` | 兼容提醒、弃用提示 |
| `--color-danger` | `#DC2626` | 破坏性变更、错误 |
| `--color-border` | `#E2E8F0` | 分割线、边框 |
| `--color-ring` | `#2563EB` | focus ring |

深色模式可以作为后续增强，但首版以浅色模式为唯一交付主题。这样能降低实现成本，并保持文档截图、代码块和表格的一致性。

### 字体

| 场景 | 字体 |
|------|------|
| UI 与正文 | `IBM Plex Sans`, `Inter`, system sans-serif |
| 标题和技术标签 | `JetBrains Mono`, `IBM Plex Sans` |
| 代码块 | `JetBrains Mono`, `Fira Code`, monospace |

字号规范：

| Token | 值 | 用途 |
|-------|----|------|
| `--text-xs` | `12px` | 标签、元信息 |
| `--text-sm` | `14px` | 导航、表格辅助信息 |
| `--text-md` | `16px` | 正文 |
| `--text-lg` | `18px` | 卡片标题 |
| `--text-xl` | `24px` | 文档二级标题 |
| `--text-2xl` | `32px` | 页面标题 |
| `--text-display` | `56px` | 官网 hero 标题 |

正文行高使用 `1.65`，代码行高使用 `1.55`。桌面文档正文宽度控制在 `760px` 到 `860px`，避免长行降低阅读效率。

### 间距与尺寸

- 基础间距单位：`8px`。
- 桌面主容器：`max-width: 1280px`。
- 最小支持宽度：`1180px`。低于该宽度允许横向滚动或提示使用桌面浏览器，不做移动布局。
- 顶栏高度：`64px`。
- 左侧文档导航宽度：`280px`。
- 右侧目录宽度：`240px`。
- API 左侧索引宽度：`320px`。
- 卡片圆角：`8px`，避免过度圆润。
- 按钮高度：`40px` 或 `44px`。

### 图标

- 使用 Lucide、Heroicons 或等价 SVG 图标。
- 同一层级图标统一 `18px` 或 `20px`，线宽统一 `1.75px`。
- 禁止使用 emoji 作为导航、标题、模块、状态或操作图标。
- 外链、复制、搜索、过滤、展开、弃用、成功、警告均使用明确 SVG 图标。

## 官网入口设计

官网是选型入口，不是长文档页。首屏需要直接回答：这是什么、适合谁、怎么安装、有哪些模块。

### 页面结构

1. Hero
   - 标题：`Under-Utils`
   - 副标题：`面向 Java 21 / Spring Boot 项目的工程模式工具包`
   - 说明：强调“不是 Hutool 替代品”，聚焦工程模式封装。
   - 主 CTA：`查看快速开始`
   - 次 CTA：`浏览 API`
   - 版本状态：稳定版、当前 SNAPSHOT、Java 21、Spring Boot 3.1.x，版本值必须来自同一构建数据源。

2. 安装区
   - 默认展示 BOM + `under-utils-spring-starter`。
   - 提供 starter 选择器：Spring、Redis、AI、兼容聚合。
   - 代码块必须有复制按钮和当前版本来源标识。

3. 模块矩阵
   - `core`、`spring`、`redis`、`http`、`ai`、`mybatis`、`biz`、starter、samples。
   - 每个模块说明一句“解决什么工程问题”，不堆砌营销词。
   - 每张模块卡提供 `文档` 和 `API` 两个入口。

4. 推荐接入路径
   - 普通 Spring Boot 服务：BOM + `under-utils-spring-starter`。
   - 需要 Redis 分布式能力：BOM + `under-utils-redis-starter`。
   - 只需要 AI：`under-utils-ai` 或 `under-utils-ai-starter`。
   - 老项目兼容：`under-utils-starter`。

5. 工程约束
   - 显示 public API 兼容策略、默认失败语义、测试要求、Maven Central 发布状态。
   - 链接到兼容性、API Review、发布指南。

### 官网设计要求

- 不使用全屏大渐变背景，避免抢过文档型产品的可读性。
- 可以使用小面积技术蓝强调色，但背景以浅灰和白色为主。
- CTA 不超过两个主按钮，避免入口过载。
- 不出现广告、赞助、合作卡片。
- 不在 hero 内放模糊装饰、动态粒子或无信息量插画。

## 使用文档设计

文档站是主工作台，目标是让用户快速完成接入、配置和故障判断。

### 布局

文档页使用三栏桌面布局：

```text
64px 顶栏
左侧导航 280px | 正文 760-860px | 右侧目录 240px
```

左侧导航固定展示一级模块，二级目录按当前模块展开。右侧目录只展示当前文章标题，不承载广告、赞助或外部推荐。

### 文档栏目

| 栏目 | 内容来源 |
|------|----------|
| 快速开始 | `README.md` 安装、环境、Starter 示例 |
| 项目边界 | `README.md` 项目边界、适合/不适合方向 |
| 模块文档 | 各 `under-utils-*/README.md` |
| 配置索引 | starter README 中的 `under.utils.*` 配置 |
| 失败语义 | Spring、Redis、HTTP、AI、Biz README 与 API Review |
| 兼容性 | `docs/COMPATIBILITY.md` |
| API 审计 | `docs/API_REVIEW.md` |
| 工程成熟度 | `docs/ENGINEERING_MATURITY.md` |
| 发布指南 | `docs/RELEASE.md` |
| 版本记录 | `CHANGELOG.md` 和 `docs/releases/*` |

### 文档组件

- 代码块：显示语言、复制按钮、版本变量来源。
- 配置表：字段、类型、默认值、作用、失败语义。
- 模块提示：标记推荐 starter、兼容入口、历史 API、弃用 API。
- 警告块：只用于破坏性变更、弃用、生产环境风险。
- 成功块：只用于已完成发布、已通过测试或推荐路径。
- 版本标记：`stable`、`snapshot`、`deprecated`、`forRemoval`。

### 文档改进点

- 文档标题不使用 emoji 前缀，改用统一 SVG 图标或文字标签。
- 避免顶部、左侧、右侧同时堆过多装饰信息。
- 搜索框始终可见，支持快捷键 `/` 聚焦。
- 代码块中的版本号必须与全局版本一致。
- 文档正文优先保留失败语义、默认行为、资源边界和测试命令。

## API 文档设计

API 文档需要保留 Javadoc 的完整性，但不能保留 iframe 和旧式固定比例 frameset。

### 实现方向

- 构建时生成 Javadoc，再将 package、class、method 索引转换为站点可检索数据。
- 页面使用统一 shell 渲染，不通过 iframe 嵌入 `overview-frame.html`、`allclasses-frame.html` 或 `overview-summary.html`。
- 保留原始 Javadoc 页面作为 fallback 下载或静态归档，但默认入口使用新 API 浏览器。

### 布局

```text
64px 顶栏
左侧 API 索引 320px | API 正文 | 右侧类内目录 260px
```

左侧索引提供：

- 模块过滤：core、spring、redis、http、ai、mybatis、biz、starter。
- 类型过滤：class、interface、annotation、enum、record。
- 状态过滤：public、deprecated、internal note。
- 搜索：支持类名、方法名、包名、注解名。

正文区域展示：

- 包名、类名、类型、所在模块。
- since、deprecated、forRemoval、替代 API。
- 构造器、方法、字段分组。
- 参数、返回值、异常和失败语义。
- 关联文档入口，例如 `SafePageQuery` 链到 MyBatis 安全文档。

右侧目录展示当前类的方法分组和锚点，不展示外部广告或无关链接。

### API 改进点

- 不再使用 iframe，保证 URL、浏览器返回、搜索和复制链接可用。
- 不使用 Javadoc 默认蓝灰旧样式，改为统一 Under-Utils 设计 token。
- deprecated API 使用黄色警告样式，说明替代路径和保留策略。
- 与 `docs/API_REVIEW.md`、`docs/COMPATIBILITY.md` 建立互链，让 public API 变更原因可追溯。
- 字体资源本地化或使用系统字体 fallback，避免字体 CSS 404。

## 统一组件规范

| 组件 | 要求 |
|------|------|
| 顶栏 | 三套前台完全一致；包含 logo、主导航、搜索、版本、GitHub |
| Logo | 使用文字标识 `Under-Utils`，可配一个简洁 SVG 符号 |
| 搜索框 | 宽 `320px`，支持 autocomplete、键盘操作和无结果建议 |
| 按钮 | 主按钮蓝底白字；次按钮白底边框；hover 不改变布局尺寸 |
| 卡片 | 8px 圆角、1px 边框、轻微 hover 边框色变化 |
| 表格 | 表头浅灰底、行高 44px、长代码使用等宽字体 |
| 代码块 | 深色代码背景 `#0F172A`，复制按钮 hover 显示 |
| 标签 | 小写或短文本，例如 `stable`、`snapshot`、`deprecated` |
| 面包屑 | 文档和 API 必须显示，官网不需要 |
| 页脚 | 只放许可证、GitHub、Maven Central、版本、构建时间 |

## 交互规范

- 所有可点击元素必须有 hover、focus、active 状态。
- focus ring 使用 `2px` `--color-ring`，不得移除。
- 搜索结果支持键盘上下选择、Enter 跳转、Esc 关闭。
- 复制成功使用 toast，3 秒自动消失，并用 `aria-live="polite"`。
- 顶栏固定时，正文顶部必须预留 `64px` 偏移，避免锚点被遮挡。
- 页面内锚点平滑滚动，但尊重 `prefers-reduced-motion`。
- 外链统一带外链图标和 `target="_blank"` / `rel="noopener noreferrer"`。

## 性能约束

- 官网首屏 JS 控制在较小范围，非必要不引入大型动画库。
- 文档和 API 搜索索引按需加载，避免每个页面预取全部 chunk。
- 图片必须声明宽高或 `aspect-ratio`。
- 字体使用 `font-display: swap`，并提供系统字体 fallback。
- 不加载广告脚本、统计以外的第三方脚本或无关 CDN 资源。
- API 索引列表超过 500 条时使用虚拟滚动或按首字母分页。

## 可访问性要求

- 正文和按钮文本对比度达到 WCAG AA，核心正文优先接近 AAA。
- 图片必须有有效 `alt`；装饰图使用空 `alt` 并隐藏给屏幕阅读器。
- 搜索、复制、主题、展开收起等 icon-only 按钮必须有 `aria-label`。
- heading 层级按 `h1 -> h2 -> h3` 顺序组织。
- 文档和 API 页提供 skip link，允许键盘用户跳过顶栏和侧栏。
- 不依赖颜色单独表达状态，状态标签必须有文字。

## 从 Hutool 三站吸取的改进

| Hutool 观察到的问题 | Under-Utils 改进 |
|---------------------|------------------|
| 官网、文档、API 三套风格割裂 | 三套前台共用同一 shell、token 和导航 |
| 主页移动端标题裁切 | 本项目不做移动端，但明确桌面最小宽度和布局约束 |
| 文档中版本号不一致 | 全站版本从单一版本源生成 |
| 文档导航使用大量 emoji | 结构图标统一 SVG，不使用 emoji |
| 文档预取 chunk 过多 | 搜索索引和路由资源按需加载 |
| API 文档使用 iframe | API 使用统一页面渲染，不使用 iframe |
| API 字体资源 404 | 字体资源本地化或系统 fallback |
| 广告/赞助占用阅读区 | 不设计任何广告、赞助或推广占位 |

## 首版交付检查

- 官网、文档、API 三套前台顶栏完全一致。
- 任一模块卡都能进入对应文档和 API。
- 安装片段只出现当前稳定版本，且来自同一版本源。
- 全站无 emoji 结构图标。
- 全站无广告位、赞助位、推广横幅。
- API 文档默认入口不包含 iframe。
- 文档和 API 搜索支持无结果建议。
- 1280、1440、1920 桌面宽度下无文本重叠、无锚点遮挡、无水平内容截断。
- 低于 1180px 不承诺适配，但不能破坏桌面布局源代码结构。
