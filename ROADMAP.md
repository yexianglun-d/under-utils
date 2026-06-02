# 路线图

本路线图用于说明 Under-Utils 的演进方向。优先级会根据 issue、真实使用反馈和维护成本调整。

## 已完成

- `v1.0.0` 已发布到 Maven Central，坐标命名空间为 `io.github.yexianglun-d`。
- GitHub Actions CI 覆盖默认编译、默认测试和 Testcontainers 集成测试。
- 接入 Central Portal 发布流程、GPG 签名 profile、sources 和 javadocs 生成。
- 清理占位模块、空占位类和过时内部规划文档。
- 明确 Under-Utils 不做 Hutool 式通用工具集合。
- 将依赖 Docker 的集成测试收口到 `under-utils-test`，通过 `integration-tests` profile 启用。
- 保证 `under-utils-samples` 默认无 Redis/MySQL 也能启动。
- 补齐 GitHub 社区文件：许可证、贡献指南、安全策略、行为准则、Issue 模板和 PR 模板。
- 将公开文档收敛为社区维护文档写法，并补齐 Redis、starter、biz 模块 README。
- 补充 starter 自动装配测试，覆盖 Redis store 切换和用户自定义 Bean 退让。
- 补充 Redis Testcontainers 测试，覆盖 cache-aside 和逻辑过期缓存行为。
- 补充 public API 兼容性策略，明确变更、弃用、配置迁移和 Release Notes 要求。
- 拆分 `under-utils-spring-starter` 与 `under-utils-redis-starter`，旧 `under-utils-starter` 保留为兼容聚合入口。
- 增加 `api-compat` profile 并接入 CI，对稳定运行时模块执行 public API 兼容性检查。
- HTTP、Spring、Redis 模块内部不再调用 `under-utils-core` 的历史 `JsonUtils`。
- 完成第一轮依赖重量审计，明确 core JSON、HTTP 客户端、Redis/Spring 耦合和 biz optional 依赖的处理顺序。
- 清理 `under-utils-biz` 未使用的 EasyExcel、POI 和 Jackson optional 依赖。
- 清理 `under-utils-http` 未实现的 HttpClient5 optional 依赖。
- 新增 `under-utils-ai` 与 `under-utils-ai-starter`，完成 OpenAI-compatible 同步文本对话、SSE 流式响应、provider 扩展和响应元数据封装。
- `under-utils-samples` 覆盖 AI profile、AI 流式 SSE、OpenAPI 签名/幂等/token 刷新/业务错误解码、异步导入进度查询和错误导出示例。
- Redis 缓存观测补齐内置计数指标和可选 Micrometer observer，starter 可在存在 `MeterRegistry` 时自动接入。
- Redis cache-aside 和 logical-cache 模板补齐链式单次调用入口，降低常见缓存读取调用样板代码。
- Crypto 重新建模和 core JSON 迁移均已形成独立设计备忘，避免长期停留在一句占位说明。

## 近期计划

- 持续维护 API Review，配置 key、异常语义和 starter 默认行为发生变化时同步记录。
- 继续收缩 `under-utils-core` 历史工具方法的扩张倾向。
- 维护 GitHub Discussions、`good first issue`、`help wanted` 和 `roadmap` 入口，把使用问题、功能孵化和确定任务分开。
- 为 `1.0.3` 准备 AI 命名客户端、文档索引和 GitHub Release Notes。
- 为 `2.0.0` 记录 Redis/Spring SPI 拆分方案，并按 [JSON_MODULE_MIGRATION.md](docs/JSON_MODULE_MIGRATION.md) 评估 core JSON 迁移。

## 后续方向

- 评估 AI 后续能力：工具调用、结构化输出、模型私有参数边界和更完整的错误元数据。
- 评估 `under-utils-crypto` 是否有真实业务需求；没有明确需求时继续保持暂缓。
- 评估 Redis/Spring SPI 拆分，降低 cache/lock 用户对 Spring 横切接口的被动依赖。

## 非目标

- 重建 Hutool、Apache Commons、Guava 或 JDK 已成熟覆盖的工具方法。
- 为了模块数量而新增没有测试复用边界的模块。
- 写入订单、支付、营销、会员等产品域专属流程。
