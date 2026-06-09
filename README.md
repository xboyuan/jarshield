# JarShield

Java JAR/WAR 字节码保护工具，支持 Spring Boot 3.x 嵌套 JAR（`jar:nested:`）协议。

源码：[github.com/xboyuan/jarshield](https://github.com/xboyuan/jarshield)

## Maven 坐标

```xml
<groupId>com.by2tech</groupId>
<artifactId>jarshield-maven-plugin</artifactId>
<version>1.0.0</version>
```

---

## 工作原理（简要）

1. **加密阶段**（`package`）：匹配到的 `.class` 方法体被清空，真实字节码 AES 加密后存入 `META-INF/.classes/`；同时将 javaagent 代码写入 fat jar。
2. **运行阶段**：通过 `-javaagent` 在类加载时解密并还原字节码，反编译只能看到空方法体。

加密产物默认命名为 `{finalName}-encrypted.jar`（与原 jar 同目录）。

---

## Maven 插件配置

### 完整示例

```xml
<plugin>
    <groupId>com.by2tech</groupId>
    <artifactId>jarshield-maven-plugin</artifactId>
    <version>1.0.0</version>
    <configuration>
        <!-- 见下方「配置项说明」 -->
        <skip>true</skip>
        <password>#</password>
        <packages>com.example</packages>
        <libjars>my-lib-</libjars>
        <excludes>com.example.config.MyConfiguration</excludes>
        <cfgfiles>application.yml,application-*.yml</cfgfiles>
    </configuration>
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

### 配置项说明

| 配置项 | Maven 属性 | 默认值 | 说明 |
|---|---|---|---|
| `skip` | `jar.encrypt.skip` | `true` | 是否跳过加密。`true` 时不生成 `-encrypted.jar`，日常开发保持默认即可 |
| `password` | `jarshield.password` | `#` | 加密密码，见下方「密码模式」。**`#` 表示无密码模式** |
| `packages` | `jarshield.packages` | — | 要加密的**类全限定名前缀**，逗号分隔。如 `com.example` 会匹配 `com.example.service.UserService`。Spring Boot 项目填业务包名 |
| `libjars` | `jarshield.libjars` | — | fat jar 内 `BOOT-INF/lib/`（或 war 的 `WEB-INF/lib/`）中**需要一并解包加密的依赖 jar 名前缀**，逗号分隔。如 `my-lib-` 匹配 `my-lib-core-1.0.0.jar`。插件会自动包含主工程 classes，无需额外配置 |
| `excludes` | `jarshield.excludes` | — | **排除**不加密的类名（前缀匹配），逗号分隔。如某些必须保留源码结构的配置类 |
| `cfgfiles` | `jarshield.cfgfiles` | — | 需要**加密**的配置文件（相对 classes 路径），逗号分隔，支持通配。如 `application.yml,*.properties` |
| `classpath` | `jarshield.classpath` | — | 加密时额外需要的**外部 jar 目录**（帮助 javassist 解析类依赖），一般 Spring Boot fat jar 可不填 |
| `code` | `jarshield.code` | — | **机器码绑定**：加密时写入机器特征，运行时 jar 只能在该机器启动。先用 CLI `-C` 生成机器码 |
| `debug` | `jarshield.debug` | `false` | 调试日志 |

> 命令行覆盖配置：`-Djarshield.password=xxx`、`-Djar.encrypt.skip=false` 等。

### 推荐：父 POM 统一管理

在父工程 `pluginManagement` 中声明默认配置，子模块只引用插件即可：

```xml
<properties>
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
    <!-- 无密码加密 -->
    <profile>
        <id>jar-encrypt</id>
        <activation>
            <property><name>jar.encrypt</name><value>true</value></property>
        </activation>
        <properties><jar.encrypt.skip>false</jar.encrypt.skip></properties>
    </profile>
    <!-- 密码模式加密（构建时指定密码） -->
    <profile>
        <id>jar-encrypt-pwd</id>
        <activation>
            <property><name>jar.encrypt.pwd</name><value>true</value></property>
        </activation>
        <properties><jar.encrypt.skip>false</jar.encrypt.skip></properties>
    </profile>
</profiles>
```

---

## 密码模式

JarShield 支持两种模式，由加密时的 `password` 决定。

### 模式一：无密码模式（推荐开发 / 内网）

| 项 | 说明 |
|---|---|
| 加密配置 | `<password>#</password>`（默认值） |
| 原理 | 加密时自动生成随机密钥并**写入 jar 内部**，运行时 agent 自动读取，无需人工输入 |
| 构建命令 | `mvn clean package -Djar.encrypt=true -DskipTests` |
| 启动命令 | `java -javaagent:app-encrypted.jar -jar app-encrypted.jar` |
| 安全级别 | 防止 casual 反编译；密钥在 jar 内，**无法防 determined 逆向** |

### 模式二：密码模式（推荐生产）

| 项 | 说明 |
|---|---|
| 加密配置 | 构建时传入真实密码，如 `-Djarshield.password=your-secret` |
| 原理 | 密码**不写入 jar**，仅保存 hash；运行时必须提供正确密码才能解密 |
| 构建命令 | `mvn clean package -Djar.encrypt.pwd=true -Djarshield.password=your-secret -DskipTests` |
| 启动命令 | 见下方「运行时传密码的方式」 |
| 安全级别 | 高于无密码模式；密码由运维保管，不随 jar 分发 |

### 运行时传密码的方式（密码模式）

优先级从高到低：

```bash
# 1. javaagent 参数直接传密码
java -javaagent:app-encrypted.jar=-pwd your-secret -jar app-encrypted.jar

# 2. 环境变量（Docker/K8s 推荐，避免密码出现在进程列表）
java -javaagent:app-encrypted.jar=-pwdname JARSHIELD_PASSWORD -jar app-encrypted.jar
# 配合：export JARSHIELD_PASSWORD=your-secret

# 3. 同目录密码文件 app-jarShield.txt 或 jarShield.txt（仅存放密码一行）

# 4. 控制台交互输入（无 TTY 的容器中会失败）
```

Docker 示例（密码模式）：

```dockerfile
ENV JARSHIELD_PASSWORD=your-secret
ENTRYPOINT ["sh", "-c", "java \
  -javaagent:/app/app-encrypted.jar=-pwdname JARSHIELD_PASSWORD \
  -jar /app/app-encrypted.jar"]
```

---

## 构建与运行速查

### 日常开发（不加密，默认）

```bash
mvn clean package -DskipTests
# 产物：target/app-1.0.0.jar（明文，无 -encrypted 后缀）
```

### 无密码加密打包

```bash
mvn clean package -Djar.encrypt=true -DskipTests
# 产物：target/app-1.0.0-encrypted.jar
```

### 密码模式加密打包

```bash
mvn clean package -Djar.encrypt.pwd=true -Djarshield.password=your-secret -DskipTests
# 产物：target/app-1.0.0-encrypted.jar
```

### 单模块

```bash
mvn clean package -pl your-module-server -Djar.encrypt=true -DskipTests
```

---

## CLI 模式（无需 Maven）

适合 CI 外挂加密、非 Maven 项目、或本地快速试加密。

### 构建 CLI 工具

```bash
mvn clean package -DskipTests
# 产物：jarshield-fatjar/target/jarshield-fatjar-1.0.0.jar
```

### 交互式加密

```bash
java -jar jarshield-fatjar-1.0.0.jar
# 按提示输入 jar 路径、包名、密码等
```

### 命令行加密

```bash
# 无密码模式
java -jar jarshield-fatjar-1.0.0.jar \
  -file app.jar \
  -packages com.example \
  -libjars my-lib- \
  -pwd "#" \
  -Y

# 密码模式
java -jar jarshield-fatjar-1.0.0.jar \
  -file app.jar \
  -packages com.example \
  -libjars my-lib- \
  -pwd your-secret \
  -Y
```

### CLI 参数对照

| 参数 | 说明 |
|---|---|
| `-file` | 待加密的 jar/war 路径 |
| `-packages` | 加密包名前缀，逗号分隔 |
| `-libjars` | lib 下要加密的 jar 名前缀，逗号分隔 |
| `-exclude` | 排除的类名 |
| `-cfgfiles` | 要加密的配置文件 |
| `-classpath` | 外部依赖 jar 目录 |
| `-pwd` | 密码，`#` 为无密码模式 |
| `-code` | 机器码 |
| `-Y` | 跳过确认 |
| `-C` | 生成机器码到 jarShield-code.txt |
| `-debug` | 调试输出 |

### 运行 CLI 加密产物

与 Maven 插件产物相同：

```bash
# 无密码模式
java -javaagent:app-encrypted.jar -jar app-encrypted.jar

# 密码模式
java -javaagent:app-encrypted.jar=-pwd your-secret -jar app-encrypted.jar
```

---

## 模块

| 模块 | 说明 |
|---|---|
| `jarshield-core` | 核心加密/解密与 javaagent |
| `jarshield-maven-plugin` | Maven 打包插件 |
| `jarshield-fatjar` | 命令行独立工具（shade 胖 jar） |

---

## 致谢

本项目基于 [ClassFinal](https://github.com/roseboy/classfinal)（Apache 2.0, roseboy）fork 并增强，详见 [NOTICE](NOTICE)。
