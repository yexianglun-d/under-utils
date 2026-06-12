import { generatedDocEntries } from './generatedDocs';

export type DocLink = {
  title: string;
  description: string;
  href: string;
  source: string;
  group: 'start' | 'module' | 'governance' | 'release';
};

const curatedDocLinks: DocLink[] = [
  {
    title: '快速开始',
    description: '环境要求、BOM 引入、轻量 starter 选择和本地开发命令。',
    href: '/docs/source/overview/#安装',
    source: 'README.md',
    group: 'start'
  },
  {
    title: '项目边界',
    description: '哪些能力适合进入 Under-Utils，哪些能力不应继续扩展。',
    href: '/docs/source/overview/#项目边界',
    source: 'README.md',
    group: 'start'
  },
  {
    title: '模块选择',
    description: '按场景选择最少 starter，明确兼容聚合入口和独立模块边界。',
    href: '/docs/source/module-selection/',
    source: 'docs/MODULE_SELECTION.md',
    group: 'start'
  },
  {
    title: '兼容性策略',
    description: '1.0.x、1.x.0、2.0.0 的版本语义、public API 范围和弃用流程。',
    href: '/docs/source/compatibility/',
    source: 'docs/COMPATIBILITY.md',
    group: 'governance'
  },
  {
    title: 'API Review',
    description: '持续收敛命名、配置 key、异常语义和模块边界的审计结论。',
    href: '/docs/source/api-review/',
    source: 'docs/API_REVIEW.md',
    group: 'governance'
  },
  {
    title: '工程成熟度',
    description: '生命周期、指标、回归测试、CHANGELOG 和 API 兼容门禁。',
    href: '/docs/source/engineering-maturity/',
    source: 'docs/ENGINEERING_MATURITY.md',
    group: 'governance'
  },
  {
    title: '发布指南',
    description: 'Maven Central Portal、GPG/PGP 签名、release profile 和 CI 发布要求。',
    href: '/docs/source/release/',
    source: 'docs/RELEASE.md',
    group: 'release'
  }
];

const generatedDocLinks = generatedDocEntries
  .filter((entry) => entry.category === 'guide' || entry.category === 'release')
  .map((entry): DocLink => ({
    title: entry.title,
    description: entry.summary,
    href: entry.href,
    source: entry.source,
    group: entry.category === 'release' ? 'release' : 'governance'
  }));

const seenSources = new Set<string>();

export const docLinks: DocLink[] = [...curatedDocLinks, ...generatedDocLinks].filter((link) => {
  if (seenSources.has(link.source)) {
    return false;
  }
  seenSources.add(link.source);
  return true;
});
