<h2 align="center">Macula Boot</h2>

<p align="center">
  <strong>基于 Spring Boot 与 Spring Cloud 的模块化微服务开发框架</strong>
</p>

<p align="center">
  <a href="https://github.com/macula-projects/macula-boot/blob/main/LICENSE">
    <img src="https://img.shields.io/github/license/macula-projects/macula-boot.svg" alt="License">
  </a>
  <a href="https://github.com/macula-projects/macula-boot/actions/workflows/verification.yml">
    <img src="https://img.shields.io/github/actions/workflow/status/macula-projects/macula-boot/verification.yml?branch=main&label=verification&logo=github" alt="Verification">
  </a>
  <a href="https://central.sonatype.com/search?q=g%3Adev.macula.boot">
    <img src="https://img.shields.io/maven-central/v/dev.macula.boot/macula-boot-parent" alt="Maven Central">
  </a>
  <img src="https://img.shields.io/badge/JDK-17+-green.svg" alt="JDK 17+">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5+-green.svg" alt="Spring Boot 3.5+">
  <img src="https://img.shields.io/badge/Spring%20Cloud-2025.x-green.svg" alt="Spring Cloud 2025.x">
</p>

Macula Boot 面向 Java 微服务应用提供统一的依赖管理、自动配置和基础能力 Starter。项目同时支持 Spring Cloud Alibaba、Spring Cloud Tencent 等技术体系，业务项目可以按需引入模块，不必绑定完整平台。

> Macula Boot 是 Macula 微服务平台的开发框架部分。通用技术服务参见 [Macula Cloud](https://github.com/macula-projects/macula-cloud)，管理端参见 [Macula Cloud Admin](https://github.com/macula-projects/macula-cloud-admin)。

## 核心能力

| 分类 | Starter | 能力 |
| --- | --- | --- |
| 基础 | `commons`、`async`、`mapstruct` | 通用模型与异常、异步上下文传递、对象转换 |
| Web 与安全 | `web`、`security`、`feign`、`springdoc`、`crypto` | Web 自动配置、认证鉴权、服务调用、OpenAPI、加解密 |
| 数据访问 | `mybatis-plus`、`jpa`、`redis`、`cache` | 关系型数据库访问、Redis 与多级缓存 |
| 分布式能力 | `lock4j`、`idempotent`、`leaderelection`、`seata`、`springretry` | 分布式锁、幂等、Leader 选举、事务与重试 |
| 消息与任务 | `rocketmq`、`kafka`、`sender`、`task` | 消息队列、可靠消息、任务调度 |
| 云原生 | `cloud-gateway`、`cloud-alibaba`、`cloud-tencent` | 网关、Nacos/Sentinel、Polaris 等服务治理能力 |
| 可观测性 | `auditlog`、`operationlog`、`prometheus`、`skywalking`、`sleuth`、`logstash` | 审计、操作日志、指标、链路追踪和日志采集 |
| 业务集成 | `oss`、`tinyid`、`binlog4j`、`websocket`、`wechat` | 对象存储、ID、Binlog、WebSocket、微信生态 |

完整模块位于 [`macula-boot-starters`](./macula-boot-starters)，每个主要 Starter 的目录中均提供独立 README 和配置示例。

## 项目结构

```text
macula-boot
├── macula-boot-parent       # 依赖版本、插件和构建规范
├── macula-boot-commons      # 通用模型、异常、上下文和工具
├── macula-boot-starters     # 按能力拆分的 Spring Boot Starter
├── macula-boot-examples     # 网关、服务、DDD、任务等示例
└── macula-boot-archetype    # Maven 项目骨架
```

## 快速使用

业务项目通常继承 Macula Boot Parent，再按需添加 Starter：

```xml
<parent>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-parent</artifactId>
    <version>6.0.1-SNAPSHOT</version>
</parent>

<dependencies>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-web</artifactId>
    </dependency>
</dependencies>
```

将示例版本替换为项目实际采用的 Macula Boot 发布版本。具体属性与扩展点请查看对应 Starter 的 README，完整应用组合可参考 [`macula-boot-examples`](./macula-boot-examples)。

## 构建与测试

项目要求 JDK 17，并使用 Maven 构建。日常开发建议从目标模块开始验证：

```bash
# 运行目标模块及其依赖的单元测试
mvn -pl macula-boot-starters/macula-boot-starter-web -am test

# 运行全仓库单元测试
mvn test

# 运行单元测试、集成测试并完成构建验证
mvn verify
```

### 测试分层规范

项目通过 Maven Surefire 和 Failsafe 区分两类测试：

| 类型 | 命名 | 使用场景 | 执行命令 |
| --- | --- | --- | --- |
| 单元测试 | `*Test`、`*Tests`、`Test*`、`*TestCase` | 纯逻辑、Mockito，以及使用 `ApplicationContextRunner` / `WebApplicationContextRunner` 的轻量自动配置测试 | `mvn test` |
| 集成测试 | `*IT`、`IT*`、`*ITCase` | `@SpringBootTest` 完整上下文、自动配置发现，以及 Redis、数据库、消息队列等基础设施协作 | `mvn verify` |

编写测试时遵循以下约定：

- 纯业务逻辑优先使用普通 JUnit 和 Mockito，不启动 Spring。
- Starter 的条件装配、属性绑定和 Bean 回退优先使用 Context Runner。
- `@SpringBootTest` 仅用于需要完整应用上下文的集成测试，并采用 `*IT` 命名。
- 依赖 Redis、数据库、Kafka、RocketMQ 等外部服务的测试必须归入集成测试，并明确运行条件。
- 测试应可重复、与执行顺序无关，不依赖随机值或未声明的共享状态。

测试报告分别生成在：

```text
target/surefire-reports    # 单元测试
target/failsafe-reports    # 集成测试
```

当前 CI 会提供 Redis 7 并执行 `mvn verify`。Kafka、RocketMQ 和 TinyID 的真实服务测试在基础设施未提供时带有明确的禁用原因，同时各模块保留不连接外部服务的自动配置测试。

### 发布构建

项目使用 `${revision}` 管理版本。生成用于发布的完整构建产物时需要启用 `deploy` Profile：

```bash
mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy
```

`deploy` Profile 默认跳过测试，因此发布前应先执行 `mvn verify`。`release.sh` 会涉及分支、提交、标签和远端推送，仅应在确认版本和工作区状态后使用。

## 开发约定

- Java 代码保持 JDK 17 兼容，包名沿用 `dev.macula...`。
- Java 排版由开发者编辑器负责：IntelliJ IDEA 自动使用版本化的 `.idea/codeStyles` 项目方案，`.editorconfig` 提供跨编辑器的基础设置，`.config/license-header` 是版权头参考模板。仓库不使用 Maven 自动格式化源码。
- 依赖版本统一在 [`macula-boot-parent/pom.xml`](./macula-boot-parent/pom.xml) 管理，避免在子模块重复声明。
- Starter 自动配置应可禁用、可覆盖，并使用 `@ConditionalOnMissingBean` 等机制为业务项目保留扩展能力。
- 新增或移动自动配置时，同步维护 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 公共 API、配置键或行为发生变化时，同步更新模块 README 和测试。

CI 使用 `.github/workflows/verification.yml` 检查相对目标分支发生变化的 Java 文件。提交前也可以手动检查全仓源码，用于逐步治理历史问题（当前全仓仍有存量告警）：

```bash
mvn -N checkstyle:check
```

IntelliJ IDEA 打开项目后会自动读取 `.idea/codeStyles/Project.xml` 和根目录 `.editorconfig`，无需手工导入 Formatter。建议在 `Settings | Tools | Actions on Save` 中启用 `Reformat code`，或在提交前对变更文件执行 Reformat Code；不要一次性格式化全仓历史代码。Checkstyle 只做静态检查，不修改文件，主要检查版权头、类型注释、import 和基础代码结构；缩进、换行和行宽由编辑器格式器负责。

Java 注释遵循以下约定：

- 类、接口、枚举、注解、Record 及测试类必须使用类级 Javadoc 说明职责，内部辅助类型也不能只写普通行注释。
- 顶层类型的 Javadoc 必须包含非空的 `@author`、`@since`；内部类型无需重复 `@author`，但仍须包含 `@since`，格式参考 `AsyncAutoConfiguration`。
- 对外公开的类和方法应说明用途、参数、返回值、异常及重要约束；实现显而易见时不写逐行翻译代码的注释。
- `TODO` 应说明待办事项和上下文，避免无意义的占位注释；修改行为时同步更新关联注释。

更多路线规划参见 [Roadmap](./Roadmap.md)。

## 技术架构

![Macula 技术架构](https://macula.dev/docs/00_introduce/%E6%A6%82%E8%BF%B0/images/macula-tech-diagram.png)

Macula Boot 特别适合拥有多条产品线、希望统一技术栈和基础组件，同时允许业务按需组合能力的研发团队。

## License

Macula Boot 基于 [Apache License 2.0](./LICENSE) 开源。
