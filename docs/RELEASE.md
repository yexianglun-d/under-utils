# 发布指南

Under-Utils 通过 Central Publisher Portal 发布到 Maven Central，不使用旧的 OSSRH / nexus-staging 流程。

官方参考：

- <https://central.sonatype.org/publish/publish-portal-maven/>
- <https://central.sonatype.org/register/namespace/>
- <https://central.sonatype.org/publish/requirements/>
- <https://central.sonatype.org/publish/requirements/gpg/>

## 发布前准备

- Central Portal 账号。
- 已验证 namespace：`io.github.yexianglun-d`。
- Central Portal user token。
- GPG 私钥、公钥已发布到 Central 支持的 key server，并安全保存 passphrase。
- `CHANGELOG.md`、README、samples 和 API Review 已反映本次发布。
- 已按 `docs/COMPATIBILITY.md` 检查兼容性影响，包括 deprecated API 和迁移说明。
- 已按版本类型准备 Release Notes：patch 版本参考 `docs/release-notes-patch-template.md`，minor 版本参考 `docs/release-notes-minor-template.md`。
- 当前发布草稿见 `docs/releases/v1.0.3.md`，正式发布前需要用实际验证结果更新其中的发布前验证段落。
- 独立 BOM 版本必须单独检查，不能只看父工程和子模块版本。

不要把 Central token、GPG 私钥、passphrase 或生成的发布 bundle 提交到仓库。

## 本地检查

```bash
mvn test
mvn -Prelease -DskipTests package
mvn -Prelease,sign-artifacts -Dgpg.skip=true -DskipTests verify
mvn -gs docs/central-dry-run-settings.xml -s docs/central-dry-run-settings.xml \
  -Papi-compat \
  -pl under-utils-core,under-utils-http,under-utils-ai,under-utils-ai-starter,under-utils-spring,under-utils-redis,under-utils-mybatis,under-utils-biz \
  -am \
  -DskipTests \
  verify
```

Central Portal dry run：

```bash
mvn -s docs/central-dry-run-settings.xml \
  -Prelease,central-publish \
  -Dcentral.publishing.server.id=central-dry-run \
  -Dcentral.skipPublishing=true \
  -Dgpg.skip=true \
  -DskipTests \
  deploy
```

`central-publish` profile 默认跳过上传。`docs/central-dry-run-settings.xml` 只提供占位凭据，因为 Central 插件在 deploy 生命周期中仍需要读取 server 配置。

`under-utils-samples` 参与构建验证，但不发布到 Maven Central。

## 凭据

本地发布前，在 `~/.m2/settings.xml` 配置 `central` server：

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>${env.CENTRAL_TOKEN_USERNAME}</username>
      <password>${env.CENTRAL_TOKEN_PASSWORD}</password>
    </server>
  </servers>
</settings>
```

server id 必须与 `central.publishing.server.id` 保持一致。

## 手动发布

上传 bundle 并等待 Central 校验：

```bash
mvn -Prelease,sign-artifacts,central-publish \
  -Dgpg.sign=true \
  -Dcentral.skipPublishing=false \
  -Dcentral.autoPublish=false \
  -Dcentral.waitUntil=validated \
  -DskipTests \
  deploy
```

校验通过后，进入 Central Portal 检查 deployment，再手动点击 Publish。

Maven Central 已发布构件不能修改或删除，修复必须通过新版本发布。

## GitHub Actions

`.github/workflows/release.yml` 提供手动发布工作流。

需要配置以下 repository secrets：

- `CENTRAL_TOKEN_USERNAME`
- `CENTRAL_TOKEN_PASSWORD`
- `GPG_PRIVATE_KEY`
- `GPG_PASSPHRASE`

发布 workflow 应绑定受保护的 `maven-central` environment。常规发布使用 `validated` 模式，保留人工 Publish 步骤。只有在版本号、namespace、签名、changelog 和回滚策略都确认后，才使用 `published` 自动发布模式。
