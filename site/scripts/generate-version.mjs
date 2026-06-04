import { readFile, mkdir, readdir, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const pom = await readFile(resolve(root, 'pom.xml'), 'utf8');
const versionMatch = pom.match(/<artifactId>under-utils<\/artifactId>\s*<version>([^<]+)<\/version>/);

if (!versionMatch) {
  throw new Error('Unable to read under-utils version from root pom.xml');
}

const version = versionMatch[1].trim();
const stable = await latestReleaseVersion();
const data = {
  stable,
  snapshot: version,
  java: '21',
  springBoot: '3.1.x',
  maven: '3.9+'
};

const output = resolve(root, 'site/src/data/version.json');
await mkdir(dirname(output), { recursive: true });
await writeFile(output, `${JSON.stringify(data, null, 2)}\n`, 'utf8');

async function latestReleaseVersion() {
  const releaseDir = resolve(root, 'docs/releases');
  const files = await readdir(releaseDir);
  const versions = files
    .map((file) => file.match(/^v(\d+\.\d+\.\d+)\.md$/)?.[1])
    .filter(Boolean)
    .sort(compareSemver);

  if (versions.length > 0) {
    return versions[versions.length - 1];
  }

  return version.replace(/-SNAPSHOT$/, '');
}

function compareSemver(left, right) {
  const leftParts = left.split('.').map(Number);
  const rightParts = right.split('.').map(Number);
  for (let index = 0; index < Math.max(leftParts.length, rightParts.length); index += 1) {
    const diff = (leftParts[index] ?? 0) - (rightParts[index] ?? 0);
    if (diff !== 0) {
      return diff;
    }
  }
  return 0;
}
