# 提交规范

本项目采用 [Conventional Commits](https://www.conventionalcommits.org/zh-hans/) 规范，便于生成 CHANGELOG、自动版本号及 Code Review。

## 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

| 部分 | 必填 | 说明 |
|:--|:--|:--|
| `type` | 是 | 变更类型 |
| `scope` | 否 | 影响模块，见下方枚举 |
| `subject` | 是 | 简短描述，祈使句，**不超过 50 字符**，**句末不加句号** |
| `body` | 否 | 详细说明（why / what），每行建议不超过 72 字符 |
| `footer` | 否 | 关联 Issue、`BREAKING CHANGE` 等 |

## Type 类型

| type | 说明 |
|:--|:--|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 仅文档变更 |
| `style` | 代码格式（不影响逻辑，如空格、缩进） |
| `refactor` | 重构（既不是 feat 也不是 fix） |
| `perf` | 性能优化 |
| `test` | 测试相关 |
| `build` | 构建系统或外部依赖（Maven、Gradle 等） |
| `ci` | CI 配置文件或脚本 |
| `chore` | 其他杂项（不改 src/test 业务逻辑） |
| `revert` | 回滚某次提交 |

## Scope 范围

| scope | 对应模块 / 目录 |
|:--|:--|
| `core` | `jarshield-core` |
| `plugin` | `jarshield-maven-plugin` |
| `fatjar` | `jarshield-fatjar` |
| `docs` | `README.md`、`doc/`、`CONTRIBUTING.md` 等 |
| `ci` | `.github/`、GitHub Actions |
| `deps` | 依赖版本升级 |

scope 可省略；跨多模块改动时选主要模块，或在 body 中说明。

## 示例

**新功能：**

```
feat(core): support jar:nested: path on Spring Boot 3
```

**修复：**

```
fix(agent): print branded message when password is missing
```

**文档：**

```
docs(readme): add gradient title and usage guide
```

**含 body 与 Issue 关联：**

```
fix(plugin): skip encryption when jar.encrypt.skip is true

Previously the plugin ran even when skip was explicitly set in profile.
Closes #12
```

**破坏性变更：**

```
feat(core)!: require JDK 17 for encrypted Spring Boot apps

BREAKING CHANGE: runtime agent no longer supports JDK 8 deployments.
```

## 推荐 / 不推荐

| 推荐 | 不推荐 |
|:--|:--|
| `feat(plugin): add cfgfiles encryption option` | `update`、`fix bug`、`修改了一下` |
| `fix(core): resolve nested jar root path` | `WIP`、`temp`、`test commit` |
| `docs: update Maven plugin configuration` | 无 type 的任意描述 |
| 一条 commit 只做一件事 | 把无关 README 和 core 重构混在一个 commit |

## 分支与合并建议

- 默认分支：`master`
- 功能 / 修复建议在分支开发，通过 Pull Request 合并
- 合并前尽量 **rebase / squash**，保持 `master` 历史清晰
- 避免将临时验证类提交（如 `chore: verify xxx`）长期留在主分支

## 本地提交示例

```bash
git add jarshield-core/src/main/java/com/by2tech/jarshield/CoreAgent.java
git commit -m "fix(agent): show colored banner on startup failure"
```

多行 body：

```bash
git commit -m "feat(plugin): support jarShield goal in package phase" -m "Allow skip flag to be controlled via jar.encrypt.skip property."
```

## Revert 提交

```
revert: feat(plugin): support cfgfiles encryption

This reverts commit abc1234.
```

---

如有疑问，可在 [Issue](https://github.com/xboyuan/jarshield/issues) 中讨论。
