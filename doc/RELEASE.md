# 发布指南

推送 `v*` 标签后，GitHub Actions 会自动：

1. `mvn deploy -Pdeploy-central` → 发布到 **Maven Central**
2. 上传 jar 到 **GitHub Release**

## 发布步骤

1. 确认 `pom.xml` 中 `<version>` 与待发版本一致（如 `1.0.0`）
2. 提交并推送到 `master`
3. 打标签并推送（标签名必须带 `v` 前缀，且与 pom 版本一致）：

```bash
git tag v1.0.0
git push origin v1.0.0
```

4. 在 GitHub **Actions** 查看 `Release` 工作流
5. 在 [Maven Central](https://central.sonatype.com/) 确认构件已发布（通常需数分钟到数小时同步到中央仓库）

## GitHub Secrets 配置

仓库 **Settings → Secrets and variables → Actions** 中添加：

| Secret | 说明 |
|:--|:--|
| `CENTRAL_USERNAME` | [Sonatype Central Portal](https://central.sonatype.com/) 用户名 |
| `CENTRAL_PASSWORD` | Central Portal 生成的 **User Token**（非登录密码） |
| `GPG_PRIVATE_KEY` | ASCII armored 私钥（`gpg --armor --export-secret-keys KEY_ID`） |
| `GPG_PASSPHRASE` | GPG 私钥口令 |

`actions/setup-java` 会自动写入 `~/.m2/settings.xml` 中的 `central` server 与 GPG 配置。

## 本地发布（可选）

与 CI 相同命令，需本机已配置 GPG 及 Central Token：

```bash
mvn clean deploy -Pdeploy-central -DskipTests
```

内网私服发布仍使用独立 profile：

```bash
mvn clean deploy -Pdeploy-private -DskipTests
```

## GitHub Release 产物

仅上传 CLI 胖 jar（Maven 依赖请走 Central，无需从 Release 下载）：

| 文件 | 说明 |
|:--|:--|
| `jarshield-fatjar-{version}.jar` | CLI 可执行胖 jar（`java -jar`） |
