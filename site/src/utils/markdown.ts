import { Marked, Renderer } from 'marked';
import { generatedDocEntries } from '../data/generatedDocs';

const renderer = new Renderer();
const docHrefBySource = new Map(generatedDocEntries.map((entry) => [entry.source, entry.href]));

renderer.heading = function heading(token: { depth: number; tokens: unknown[]; text: string }) {
  const parser = (this as unknown as { parser: { parseInline: (tokens: unknown[]) => string } }).parser;
  const text = token.tokens ? parser.parseInline(token.tokens) : token.text;
  const id = slugify(stripHtml(text));
  return `<h${token.depth} id="${id}">${text}</h${token.depth}>`;
};

renderer.link = function link(token: { href: string; title?: string | null; tokens: unknown[] }) {
  const parser = (this as unknown as { parser: { parseInline: (tokens: unknown[]) => string } }).parser;
  const text = parser.parseInline(token.tokens);
  const href = resolveMarkdownHref(token.href);
  const title = token.title ? ` title="${escapeHtml(token.title)}"` : '';
  return `<a href="${escapeHtml(href)}"${title}>${text}</a>`;
};

const marked = new Marked({
  async: false,
  breaks: false,
  gfm: true,
  renderer
});

export function renderMarkdown(markdown: string) {
  return marked.parse(markdown) as string;
}

export function slugify(value: string) {
  const normalized = value
    .toLowerCase()
    .replace(/[\s_/]+/g, '-')
    .replace(/[^a-z0-9\u4e00-\u9fa5.-]+/g, '')
    .replace(/[.]+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-|-$/g, '');
  return normalized || 'section';
}

function stripHtml(value: string) {
  return value.replace(/<[^>]+>/g, '');
}

function resolveMarkdownHref(href: string) {
  if (/^(https?:|mailto:|tel:|#|\/)/i.test(href)) {
    return href;
  }

  const [rawPath, hash = ''] = href.split('#');
  const normalized = rawPath.replace(/^\.?\//, '').replaceAll('\\', '/');
  if (normalized.endsWith('.md') && docHrefBySource.has(normalized)) {
    return `${docHrefBySource.get(normalized)}${hash ? `#${hash}` : ''}`;
  }

  if (normalized.endsWith('.md')) {
    return `https://github.com/yexianglun-d/under-utils/blob/main/${normalized}${hash ? `#${hash}` : ''}`;
  }

  return `https://github.com/yexianglun-d/under-utils/blob/main/${normalized}${hash ? `#${hash}` : ''}`;
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('"', '&quot;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;');
}
