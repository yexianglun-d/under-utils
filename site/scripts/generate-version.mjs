import { readFile, mkdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const pom = await readFile(resolve(root, 'pom.xml'), 'utf8');
const versionMatch = pom.match(/<artifactId>under-utils<\/artifactId>\s*<version>([^<]+)<\/version>/);

if (!versionMatch) {
  throw new Error('Unable to read under-utils version from root pom.xml');
}

const version = versionMatch[1].trim();
const data = {
  stable: '1.0.3',
  snapshot: version,
  java: '21',
  springBoot: '3.1.x',
  maven: '3.9+'
};

const output = resolve(root, 'site/src/data/version.json');
await mkdir(dirname(output), { recursive: true });
await writeFile(output, `${JSON.stringify(data, null, 2)}\n`, 'utf8');
