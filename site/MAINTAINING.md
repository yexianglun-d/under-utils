# 官网更新指南

本文档用于记录 Under-Utils 官网后续迭代规则。每次功能、模块、文档或 public API 发生变化时，优先让 Codex 按本文档更新官网。

## 适用场景

- 新增或调整 Java public/protected API。
- 新增模块、starter、示例工程或测试模块。
- 更新 README、模块 README、发布说明、兼容性策略或工程文档。
- 调整官网首页、文档站、API 浏览器的前台展示。
- 发布新版本，需要同步稳定版本、snapshot、Maven 坐标和发布说明。

## 官网文件结构

| 类型 | 文件 |
|------|------|
| 首页 | `site/src/pages/index.astro` |
| 文档首页 | `site/src/pages/docs/index.astro` |
| 模块页 | `site/src/pages/docs/modules/[slug].astro` |
| Markdown 源文档页 | `site/src/pages/docs/source/[slug].astro` |
| API 浏览器 | `site/src/pages/api/index.astro` |
| 模块配置 | `site/src/data/modules.ts` |
| 全局搜索 | `site/src/data/search.ts` |
| 全局样式与动效 | `site/src/styles/global.css` |
| API 抽取工具 | `site/tools/ApiIndexExtractor.java` |
| API 生成脚本 | `site/scripts/generate-api-index.mjs` |
| 文档生成脚本 | `site/scripts/generate-docs-index.mjs` |
| 版本生成脚本 | `site/scripts/generate-version.mjs` |

## 内容更新规则

### 更新已有功能

1. 先更新真实源文档：
   - 根说明：`README.md`
   - 模块说明：`under-utils-*/README.md`
   - 工程文档：`docs/*.md`
   - 发布说明：`docs/releases/*.md`
2. 再运行官网构建，让站点重新生成文档索引和 API 索引。
3. 不要直接手写 `site/src/data/docs-index.generated.json` 或 `site/src/data/api-index.generated.json`，除非只是临时排查。

### 新增模块

新增模块时至少同步：

1. 新模块自身 `README.md`。
2. `site/src/data/modules.ts` 中新增模块卡片信息。
3. `site/tools/ApiIndexExtractor.java` 中新增 artifact 到模块 slug 的映射。
4. 如有 starter，确认 API 页筛选参数和模块页路径一致。

### 新增 public API

只要 Java 源码在已登记模块的 `src/main/java` 下，并且类型是 public，构建会自动收录。

需要确认：

- 类、接口、注解、枚举有清晰 Javadoc 首句。
- 重要 public/protected 方法有 Javadoc 首句。
- 弃用 API 使用 `@Deprecated` 和 `@deprecated` Javadoc。
- `@since` 与版本规划一致。

### 更新首页

首页只保留决策入口和产品识别，不放长解释文案。

允许内容：

- 产品名、版本、运行要求。
- 核心能力短标签。
- 快速开始、API、文档入口。
- Maven 坐标。
- 模块入口。

不建议内容：

- 大段产品介绍。
- 广告位、赞助位、无关宣传。
- 移动端专门适配。
- 与 README 重复的大量说明。

## 更新命令

官网构建要求 Node.js `>=22.12.0`。如果系统默认 Node 版本较低，需要先切换到符合要求的 Node 运行时。

```bash
cd /Users/deng/Desktop/Java开发工具包/java-tool-box/site
npm run build
```

构建会自动执行：

```bash
node scripts/generate-version.mjs
node scripts/generate-api-index.mjs
node scripts/generate-docs-index.mjs
astro build
```

本地预览：

```bash
cd /Users/deng/Desktop/Java开发工具包/java-tool-box/site
./node_modules/.bin/astro dev --host 127.0.0.1 --port 4323
```

## 验证清单

每次官网更新后至少验证：

- `npm run build` 通过。
- `npm audit --omit=dev` 无高风险问题。
- 首页、文档页、API 页可访问。
- 新增文档能在 `/docs/source/` 或模块页中看到。
- 新增 API 能在 `/api/` 搜索到。
- 首页没有 Astro dev toolbar。
- 首页没有解释性长文案回流。
- 站内链接没有明显 404。

如同时修改 Java 代码，还需要按 Java 改动范围运行对应 Maven 测试。

## Codex 更新提示词模板

以后可以直接这样让 Codex 更新官网：

```text
请根据 site/MAINTAINING.md 更新官网。
本次功能变化：
1. ...
2. ...
需要同步：
- README / 模块 README / 发布说明
- 官网首页 / 文档页 / API 页
- 运行构建和必要验证
```

如果是新增模块：

```text
请根据 site/MAINTAINING.md 为新模块 <模块名> 更新官网。
模块坐标：
- artifactId: ...
- Java package: ...
- README 路径: ...
要求同步模块卡片、模块文档、API 抽取映射，并运行官网构建验证。
```

如果是版本发布：

```text
请根据 site/MAINTAINING.md 更新官网版本信息。
发布版本：...
主要变化：
1. ...
2. ...
要求同步 README、docs/releases、官网版本数据、首页展示和文档索引，并运行构建验证。
```

## 发布产物

静态产物目录：

```text
site/dist
```

可部署到 Nginx、OSS、GitHub Pages 或其他静态托管服务。

## 官网域名

当前官网域名：

```text
https://under-utils.howied.me
```

项目侧配置：

- `site/astro.config.mjs` 的 `site` 固定为 `https://under-utils.howied.me`。
- `site/public/CNAME` 用于 GitHub Pages 等静态托管识别自定义域名。
- 构建后 `site/dist/CNAME` 会随静态产物一起发布。

阿里云域名解析建议：

| 场景 | 主机记录 | 记录类型 | 记录值 |
|------|----------|----------|--------|
| GitHub Pages | `under-utils` | `CNAME` | `yexianglun-d.github.io` |
| 自有服务器 / Nginx | `under-utils` | `A` | 服务器公网 IP |
| OSS / CDN / 其他静态托管 | `under-utils` | `CNAME` | 托管平台提供的域名 |

如果部署目标不是 GitHub Pages，需要把解析记录值改成实际托管平台给出的域名或 IP，并为 `under-utils.howied.me` 申请 HTTPS 证书。

域名发布验证：

```bash
curl -I https://under-utils.howied.me/
```

如果更换官网域名，需要同步更新：

- `site/astro.config.mjs`
- `site/public/CNAME`
- 本文档的官网域名和解析记录
