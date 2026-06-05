import { mkdir, readdir, readFile, stat, writeFile } from 'node:fs/promises';
import { basename, dirname, extname, join, relative, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const siteRoot = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repoRoot = resolve(siteRoot, '..');
const output = resolve(siteRoot, 'src/data/docs-index.generated.json');

const moduleSlugs = new Map([
  ['under-utils-core', 'core'],
  ['under-utils-spring', 'spring'],
  ['under-utils-redis', 'redis'],
  ['under-utils-http', 'http'],
  ['under-utils-ai', 'ai'],
  ['under-utils-security', 'security'],
  ['under-utils-mybatis', 'mybatis'],
  ['under-utils-biz', 'biz'],
  ['under-utils-ai-starter', 'ai-starter'],
  ['under-utils-security-starter', 'security-starter'],
  ['under-utils-mybatis-starter', 'mybatis-starter'],
  ['under-utils-spring-starter', 'spring-starter'],
  ['under-utils-redis-starter', 'redis-starter'],
  ['under-utils-starter', 'starter'],
  ['under-utils-samples', 'samples'],
  ['under-utils-test', 'test']
]);

const excludedDocs = new Set([
  'docs/FRONTEND_DESIGN.md',
  'docs/release-notes-minor-template.md',
  'docs/release-notes-patch-template.md'
]);

const docFiles = [
  { path: 'README.md', slug: 'overview', category: 'overview', href: '/docs/source/overview/' },
  ...(await collectMarkdown('docs')).map((path) => {
    const release = path.startsWith('docs/releases/');
    const slug = release
      ? `release-${basename(path, extname(path)).replaceAll('.', '-')}`
      : slugify(basename(path, extname(path)));
    return {
      path,
      slug,
      category: release ? 'release' : 'guide',
      href: `/docs/source/${slug}/`
    };
  }),
  ...Array.from(moduleSlugs.entries()).map(([artifact, slug]) => ({
    path: `${artifact}/README.md`,
    slug: `module-${slug}`,
    category: 'module',
    artifact,
    moduleSlug: slug,
    href: `/docs/modules/${slug}/#readme`
  }))
];

const entries = [];
for (const file of docFiles) {
  const absolutePath = resolve(repoRoot, file.path);
  const markdown = await readFile(absolutePath, 'utf8');
  entries.push({
    slug: file.slug,
    category: file.category,
    artifact: file.artifact ?? '',
    moduleSlug: file.moduleSlug ?? '',
    title: titleOf(markdown, file.path),
    summary: summaryOf(markdown),
    source: file.path,
    href: file.href,
    headings: headingsOf(markdown),
    codeBlockCount: (markdown.match(/```/g) ?? []).length / 2,
    markdown
  });
}

await mkdir(dirname(output), { recursive: true });
await writeFile(
  output,
  `${JSON.stringify({
    meta: {
      generatedBy: 'site/scripts/generate-docs-index.mjs',
      source: 'README.md, docs/**/*.md and under-utils-*/README.md',
      entryCount: entries.length
    },
    entries
  }, null, 2)}\n`,
  'utf8'
);

async function collectMarkdown(rootDir) {
  const root = resolve(repoRoot, rootDir);
  const files = [];

  async function walk(dir) {
    const children = await readdir(dir);
    for (const child of children) {
      const fullPath = join(dir, child);
      const info = await stat(fullPath);
      if (info.isDirectory()) {
        await walk(fullPath);
        continue;
      }
      if (!child.endsWith('.md')) {
        continue;
      }
      const rel = relative(repoRoot, fullPath).replaceAll('\\', '/');
      if (!excludedDocs.has(rel)) {
        files.push(rel);
      }
    }
  }

  await walk(root);
  return files.sort();
}

function titleOf(markdown, fallbackPath) {
  const heading = markdown.match(/^#\s+(.+)$/m)?.[1]?.trim();
  return heading || basename(fallbackPath, extname(fallbackPath));
}

function summaryOf(markdown) {
  const lines = markdown.split(/\r?\n/);
  const paragraphs = [];
  let current = [];
  let inCode = false;

  for (const line of lines) {
    if (line.startsWith('```')) {
      inCode = !inCode;
      continue;
    }
    if (inCode || shouldSkipSummaryLine(line)) {
      if (current.length) {
        paragraphs.push(current.join(' '));
        current = [];
      }
      continue;
    }
    if (!line.trim()) {
      if (current.length) {
        paragraphs.push(current.join(' '));
        current = [];
      }
      continue;
    }
    current.push(stripMarkdown(line.trim()));
  }

  if (current.length) {
    paragraphs.push(current.join(' '));
  }

  return paragraphs.find(Boolean) ?? '';
}

function shouldSkipSummaryLine(line) {
  const trimmed = line.trim();
  return !trimmed
    || trimmed.startsWith('#')
    || trimmed.startsWith('|')
    || trimmed.startsWith('[')
    || trimmed.startsWith('!')
    || trimmed.startsWith('- ')
    || trimmed.startsWith('* ')
    || trimmed.startsWith('>')
    || trimmed.includes('| [English]');
}

function headingsOf(markdown) {
  return markdown
    .split(/\r?\n/)
    .map((line) => line.match(/^(#{2,4})\s+(.+)$/))
    .filter(Boolean)
    .map((match) => {
      const text = stripMarkdown(match[2].trim());
      return {
        depth: match[1].length,
        title: text,
        id: slugify(text)
      };
    });
}

function stripMarkdown(value) {
  return value
    .replace(/`([^`]+)`/g, '$1')
    .replace(/\[([^\]]+)]\([^)]+\)/g, '$1')
    .replace(/\*\*([^*]+)\*\*/g, '$1')
    .replace(/\*([^*]+)\*/g, '$1')
    .replace(/<[^>]+>/g, '')
    .trim();
}

function slugify(value) {
  const normalized = value
    .toLowerCase()
    .replace(/[\s_/]+/g, '-')
    .replace(/[^a-z0-9\u4e00-\u9fa5.-]+/g, '')
    .replace(/[.]+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '');
  return normalized || 'section';
}
