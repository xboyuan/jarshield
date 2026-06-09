<p align="center">
  <img src="doc/jarshield-title.svg" alt="JarShield" width="420"/>
</p>

<p align="center">
  <strong>一个支持 Spring Boot 3.x + 的 JAR/WAR 字节码保护 Maven 插件</strong>
</p>

[//]: # (<p align="center">)

[//]: # (  <a href="https://github.com/xboyuan/jarshield"><img src="https://img.shields.io/github/stars/xboyuan/jarshield?style=flat-square" alt="stars"/></a>)

[//]: # (  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square" alt="license"/></a>)

[//]: # (  <a href="https://search.maven.org/search?q=g:com.by2tech"><img src="https://img.shields.io/maven-central/v/com.by2tech/jarshield-maven-plugin?style=flat-square" alt="maven"/></a>)

[//]: # (</p>)

# 简介

JarShield 是一个面向 Spring Boot 可执行 JAR/WAR 的字节码保护工具，通过 Maven 插件或 CLI 在打包阶段加密 class，运行时由 javaagent 在内存中解密。

其支持 **JDK 8+（插件/CLI 构建）、JDK 17+（Spring Boot 3.x 应用运行）**，兼容 Spring Boot 3.x 嵌套 JAR（`jar:nested:`）协议。

# 特性

- 支持 **Maven 插件** 与 **CLI** 两种使用方式，CI/CD 友好。
- 支持 **无密码模式**（`#`）与 **密码模式**，生产环境可运行时传密。
- 支持加密 **BOOT-INF/classes** 及 **BOOT-INF/lib** 下指定依赖 jar。
- 支持 **包名前缀过滤**、类排除、配置文件加密、机器码绑定。
- **完整兼容 Spring Boot 3.x** 嵌套 jar 路径解析。
- **不跳过** Controller / Service / Lambda 等业务类（保护强度高于部分同类）。
- 加密产物写入 **JarShield 启动 Banner**，便于识别受保护应用。

# 使用方法

## 模块说明

| 使用场景 | 模块 | Maven 坐标 |
|:--|:--|:--|
| Spring Boot / Maven 项目 | `jarshield-maven-plugin` | `com.by2tech:jarshield-maven-plugin:1.0.0` |
| 命令行 / 非 Maven 项目 | `jarshield-fatjar` | 本地构建 `jarshield-fatjar/target/jarshield-fatjar-1.0.0.jar` |
| 核心库（一般无需直接依赖） | `jarshield-core` | `com.by2tech:jarshield-core:1.0.0` |
## Maven引入模式
### 1. 引入 Maven 插件

父工程 `pluginManagement` 统一配置，子模块按需启用：

```xml
<properties>
    <!-- 默认不加密，日常开发保持 true -->
    <jar.encrypt.skip>true</jar.encrypt.skip>
</properties>

<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>com.by2tech</groupId>
                <artifactId>jarshield-maven-plugin</artifactId>
                <version>1.0.0</version>
                <configuration>
                    <password>#</password>
                    <packages>com.example</packages>
                    <libjars>my-lib-</libjars>
                    <skip>${jar.encrypt.skip}</skip>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>
</build>

<profiles>
    <profile>
        <id>jar-encrypt</id>
        <activation>
            <property><name>jar.encrypt</name><value>true</value></property>
        </activation>
        <properties><jar.encrypt.skip>false</jar.encrypt.skip></properties>
    </profile>
    <profile>
        <id>jar-encrypt-pwd</id>
        <activation>
            <property><name>jar.encrypt.pwd</name><value>true</value></property>
        </activation>
        <properties><jar.encrypt.skip>false</jar.encrypt.skip></properties>
    </profile>
</profiles>
```

子模块 `*-server` 中声明执行目标：

```xml
<plugin>
    <groupId>com.by2tech</groupId>
    <artifactId>jarshield-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>encrypt-jar</id>
            <phase>package</phase>
            <goals>
                <goal>jarShield</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**插件配置项：**

| 配置项 | Maven 属性 | 默认值 | 说明 |
|:--|:--|:--|:--|
| `skip` | `jar.encrypt.skip` | `true` | 是否跳过加密 |
| `password` | `jarshield.password` | `#` | 加密密码，`#` 为无密码模式 |
| `packages` | `jarshield.packages` | — | 要加密的类名前缀，逗号分隔 |
| `libjars` | `jarshield.libjars` | — | `BOOT-INF/lib/` 下需加密的 jar 名前缀 |
| `excludes` | `jarshield.excludes` | — | 排除不加密的类名 |
| `cfgfiles` | `jarshield.cfgfiles` | — | 要加密的配置文件，支持通配 |
| `classpath` | `jarshield.classpath` | — | 加密时额外 classpath 目录 |
| `code` | `jarshield.code` | — | 机器码绑定 |
| `debug` | `jarshield.debug` | `false` | 调试日志 |

## 2. 打包

**日常开发（不加密，默认）：**

```bash
mvn clean package -DskipTests
# 产物：target/app-1.0.0.jar
```

**无密码模式：**

```bash
mvn clean package -Djar.encrypt=true -DskipTests
# 产物：target/app-1.0.0-encrypted.jar
```

**密码模式：**

```bash
mvn clean package -Djar.encrypt.pwd=true -Djarshield.password=your-secret -DskipTests
# 产物：target/app-1.0.0-encrypted.jar
```

加密阶段会将匹配 class 的方法体清空，真实字节码写入 `META-INF/.classes/`，并注入 javaagent。

## 3. 运行加密 JAR

运行加密包 **必须** 挂载 javaagent（agent 已内嵌于 `-encrypted.jar`）：

**无密码模式：**

```bash
java -javaagent:app-1.0.0-encrypted.jar -jar app-1.0.0-encrypted.jar
```

**密码模式（任选其一传密）：**

```bash
# javaagent 参数
java -javaagent:app-1.0.0-encrypted.jar=-pwd your-secret -jar app-1.0.0-encrypted.jar

# 环境变量（Docker / K8s 推荐）
java -javaagent:app-1.0.0-encrypted.jar=-pwdname JARSHIELD_PASSWORD -jar app-1.0.0-encrypted.jar

# 同目录密码文件 jarShield.txt 或 {jar名}jarShield.txt
```

| 模式 | 加密配置 | 启动是否需要传密 |
|:--|:--|:--|
| 无密码 | `<password>#</password>` | 否，agent 自动读 jar 内密钥 |
| 密码 | `-Djarshield.password=***` | 是，见上方传密方式 |

Docker 示例：

```dockerfile
ENV JARSHIELD_PASSWORD=your-secret
ENTRYPOINT ["sh", "-c", "java \
  -javaagent:/app/app-encrypted.jar=-pwdname JARSHIELD_PASSWORD \
  -jar /app/app-encrypted.jar"]
```

## CLI 模式

无需 Maven 时，先构建 CLI 工具：

```bash
mvn clean package -pl jarshield-fatjar -am -DskipTests
```
或从 Release 中下载jar包   

**交互式：**

```bash
java -jar jarshield-fatjar-1.0.0.jar
```

**命令行：**

```bash
java -jar jarshield-fatjar-1.0.0.jar \
  -file app.jar \
  -packages com.example \
  -libjars my-lib- \
  -pwd your-secret \
  -Y
```

| 参数 | 说明 |
|:--|:--|
| `-file` | 待加密 jar/war 路径 |
| `-packages` | 包名前缀 |
| `-libjars` | lib 下 jar 名前缀 |
| `-pwd` | 密码，`#` 为无密码模式 |
| `-exclude` | 排除类名 |
| `-cfgfiles` | 加密配置文件 |
| `-C` | 生成机器码 |
| `-Y` | 跳过确认 |

# 致谢

本项目基于 [ClassFinal](https://github.com/roseboy/classfinal)（Apache 2.0, roseboy）fork 并增强，详见 [NOTICE](NOTICE)。
