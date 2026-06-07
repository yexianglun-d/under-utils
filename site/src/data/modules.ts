export type ModuleEntry = {
  slug: string;
  artifact: string;
  title: string;
  kind: 'starter' | 'library' | 'sample' | 'test';
  summary: string;
  recommendedFor: string;
  highlights: string[];
  docsSource: string;
  apiQuery: string;
};

export const modules: ModuleEntry[] = [
  {
    slug: 'spring-starter',
    artifact: 'under-utils-spring-starter',
    title: 'Spring Starter',
    kind: 'starter',
    summary: '请求上下文、限流、防重复提交、本地 store 和基础 SPI 自动装配。',
    recommendedFor: '普通 Spring Boot 服务的首选轻量入口。',
    highlights: ['无 Redis 依赖', '本地状态主动清理', '保持 under.utils.* 配置前缀'],
    docsSource: 'under-utils-spring-starter/README.md',
    apiQuery: 'spring'
  },
  {
    slug: 'redis-starter',
    artifact: 'under-utils-redis-starter',
    title: 'Redis Starter',
    kind: 'starter',
    summary: '接入 Redis store、分布式锁、cache-aside、逻辑过期缓存和缓存观测。',
    recommendedFor: '多实例部署、分布式锁和缓存模板场景。',
    highlights: ['依赖业务提供 RedissonClient', '缓存指标可直接读取', 'Micrometer 观测可选'],
    docsSource: 'under-utils-redis-starter/README.md',
    apiQuery: 'redis'
  },
  {
    slug: 'ai',
    artifact: 'under-utils-ai',
    title: 'AI',
    kind: 'library',
    summary: 'OpenAI-compatible AI 大模型基础调用封装，覆盖同步、流式和命名客户端。',
    recommendedFor: '需要统一 AI 调用入口和 provider 扩展的服务。',
    highlights: ['同步和流式文本对话', '多客户端注册表', '错误分类和敏感信息脱敏'],
    docsSource: 'under-utils-ai/README.md',
    apiQuery: 'ai'
  },
  {
    slug: 'ai-starter',
    artifact: 'under-utils-ai-starter',
    title: 'AI Starter',
    kind: 'starter',
    summary: 'Spring Boot AI 自动装配入口，按配置创建默认或多个命名 AiClient。',
    recommendedFor: '需要在 Spring Boot 服务中通过配置接入 OpenAI-compatible 模型的项目。',
    highlights: ['默认显式启用', '支持命名客户端注册表', '自定义 AiClientProvider 退让'],
    docsSource: 'under-utils-ai-starter/README.md',
    apiQuery: 'ai-starter'
  },
  {
    slug: 'security',
    artifact: 'under-utils-security',
    title: 'Security',
    kind: 'library',
    summary: '字段级 AES-GCM 加密、MyBatis 显式加密 TypeHandler 和响应脱敏。',
    recommendedFor: '需要落库加密、响应脱敏和安全字段治理边界的企业服务。',
    highlights: ['AES-GCM 随机 IV', 'keyId 密文 envelope', '显式 TypeHandler 接入'],
    docsSource: 'under-utils-security/README.md',
    apiQuery: 'security'
  },
  {
    slug: 'security-starter',
    artifact: 'under-utils-security-starter',
    title: 'Security Starter',
    kind: 'starter',
    summary: '按显式配置创建 FieldEncryptor，并注册 MyBatis 加密 TypeHandler 默认加密器。',
    recommendedFor: '希望通过 Spring Boot 配置接入字段加密的项目。',
    highlights: ['不做隐式全局加密', '用户 Bean 自动退让', 'Base64 AES key 显式启用'],
    docsSource: 'under-utils-security-starter/README.md',
    apiQuery: 'security-starter'
  },
  {
    slug: 'jdbc',
    artifact: 'under-utils-jdbc',
    title: 'JDBC',
    kind: 'library',
    summary: '基于 Spring JDBC 的服务层幂等状态存储，支持处理中状态、完成结果复用和过期清理。',
    recommendedFor: '已有业务数据库、不想额外引入 Redis 的多实例幂等场景。',
    highlights: ['显式建表', '表名白名单校验', 'MySQL/PostgreSQL 脚本'],
    docsSource: 'under-utils-jdbc/README.md',
    apiQuery: 'jdbc'
  },
  {
    slug: 'jdbc-starter',
    artifact: 'under-utils-jdbc-starter',
    title: 'JDBC Starter',
    kind: 'starter',
    summary: '显式选择 store=jdbc 时自动装配数据库幂等 store 和过期记录清理任务。',
    recommendedFor: '希望用业务库承载 @Idempotent 状态的 Spring Boot 服务。',
    highlights: ['独立 starter', '用户 IdempotencyStore 退让', '清理任务可关闭'],
    docsSource: 'under-utils-jdbc-starter/README.md',
    apiQuery: 'jdbc-starter'
  },
  {
    slug: 'http',
    artifact: 'under-utils-http',
    title: 'HTTP',
    kind: 'library',
    summary: 'HTTP 便捷调用与 OpenAPI 客户端治理，包括 token、签名、重试和错误解码。',
    recommendedFor: '第三方 OpenAPI 调用、幂等 header 和业务错误治理。',
    highlights: ['OpenApiClient 主线入口', 'RefreshingAccessTokenProvider', '默认不阻塞重试等待'],
    docsSource: 'under-utils-http/README.md',
    apiQuery: 'http'
  },
  {
    slug: 'redis',
    artifact: 'under-utils-redis',
    title: 'Redis',
    kind: 'library',
    summary: 'Redisson 分布式锁、限流/防重 Redis store、缓存模板和内置指标。',
    recommendedFor: '需要直接组合 Redis 组件而不使用 starter 的项目。',
    highlights: ['CacheAsideTemplate', 'LogicalExpireCacheTemplate', 'CountingCacheOperationObserver'],
    docsSource: 'under-utils-redis/README.md',
    apiQuery: 'redis'
  },
  {
    slug: 'spring',
    artifact: 'under-utils-spring',
    title: 'Spring',
    kind: 'library',
    summary: 'Spring Web 上下文传播、限流/防重抽象、返回结果、异常处理和 JSON 脱敏。',
    recommendedFor: '需要手动接线 Spring 横切能力或扩展 SPI 的项目。',
    highlights: ['OperationContextSnapshot', '@RateLimit', '@PreventRepeat'],
    docsSource: 'under-utils-spring/README.md',
    apiQuery: 'spring'
  },
  {
    slug: 'mybatis',
    artifact: 'under-utils-mybatis',
    title: 'MyBatis',
    kind: 'library',
    summary: 'MyBatis-Plus 安全分页、排序白名单、审计填充和分页结果封装。',
    recommendedFor: '需要 Web 入参安全分页和审计字段填充的项目。',
    highlights: ['SafePageQuery', 'SortFieldMapping', 'DefaultMetaObjectHandler'],
    docsSource: 'under-utils-mybatis/README.md',
    apiQuery: 'mybatis'
  },
  {
    slug: 'mybatis-starter',
    artifact: 'under-utils-mybatis-starter',
    title: 'MyBatis Starter',
    kind: 'starter',
    summary: '自动装配 MyBatis-Plus interceptor 和默认审计字段填充处理器。',
    recommendedFor: '想零配置接入 Under-Utils MyBatis 能力的 Spring Boot 项目。',
    highlights: ['独立 starter', 'ConditionalOnMissingBean 退让', 'under.utils.mybatis 配置'],
    docsSource: 'under-utils-mybatis-starter/README.md',
    apiQuery: 'mybatis-starter'
  },
  {
    slug: 'biz',
    artifact: 'under-utils-biz',
    title: 'Biz',
    kind: 'library',
    summary: '可复用业务流程模板，当前覆盖 CSV 导入、异步导入、进度查询和错误导出。',
    recommendedFor: '需要沉淀可复用导入任务流程的后台系统。',
    highlights: ['AsyncImportTaskTemplate', 'ImportProgress', 'ImportErrorExporter'],
    docsSource: 'under-utils-biz/README.md',
    apiQuery: 'biz'
  },
  {
    slug: 'core',
    artifact: 'under-utils-core',
    title: 'Core',
    kind: 'library',
    summary: '低耦合基础能力和历史工具兼容维护，主线保留雪花 ID 与金额工具。',
    recommendedFor: '需要稳定 ID 生成和金额计算语义的基础层。',
    highlights: ['IdGenerator', 'MoneyUtils', '历史工具只做兼容维护'],
    docsSource: 'under-utils-core/README.md',
    apiQuery: 'core'
  },
  {
    slug: 'starter',
    artifact: 'under-utils-starter',
    title: 'Compat Starter',
    kind: 'starter',
    summary: '兼容聚合 starter，保留旧入口，继续覆盖 Spring 与 Redis 自动装配。',
    recommendedFor: '暂时不调整依赖坐标的老项目。',
    highlights: ['兼容入口', '新项目优先选择轻量 starter', '配置 key 保持稳定'],
    docsSource: 'under-utils-starter/README.md',
    apiQuery: 'starter'
  },
  {
    slug: 'samples',
    artifact: 'under-utils-samples',
    title: 'Samples',
    kind: 'sample',
    summary: '可运行示例工程，用于验证 starter 与工程模式封装的真实使用体验。',
    recommendedFor: '本地验证配置、profile 和请求样例。',
    highlights: ['AI Profile', 'Redis Profile', '自定义存储 Profile'],
    docsSource: 'under-utils-samples/README.md',
    apiQuery: 'samples'
  },
  {
    slug: 'test',
    artifact: 'under-utils-test',
    title: 'Test',
    kind: 'test',
    summary: 'Testcontainers 集成测试模块，仅通过 integration-tests profile 启用。',
    recommendedFor: '维护者验证 Redis 等外部依赖场景。',
    highlights: ['默认测试不依赖 Docker', '集成测试按 profile 启用', '回归测试可追溯'],
    docsSource: 'under-utils-test/README.md',
    apiQuery: 'test'
  }
];

export const primaryModules = modules.filter((module) => module.kind !== 'sample' && module.kind !== 'test');
