# AGENTS.md

Macula Boot 是基于 Java 17、Spring Boot 3.5 和 Spring Cloud 2025 的多模块微服务框架。修改应保持兼容、聚焦目标模块，并遵循现有代码风格。

## 开始工作

- 先检查 `git status --short`，不要覆盖或清理用户已有改动。
- 先读目标模块的 `pom.xml`、`README.md`、相邻实现和测试，再修改代码。
- 使用 `rg` / `rg --files` 定位代码；避免无目的遍历整个仓库。
- 依赖版本统一由 `macula-boot-parent/pom.xml` 管理，根版本由 `revision` 属性管理。

## 常用命令

```bash
# 目标模块及其依赖（首选）
mvn -pl <module-path> -am test
mvn -pl <module-path> -am test -Dtest=TestClass#method -Dsurefire.failIfNoSpecifiedTests=false

# 全量测试
mvn test

# 手动运行全仓 Checkstyle；CI 检查分支中变更的 Java 文件
mvn -N checkstyle:check

# 单元测试 + 集成测试（Failsafe 执行 *IT）
mvn verify

# 指定集成测试（多模块构建允许无匹配测试的模块通过）
mvn -pl <module-path> -am verify -Dit.test=IntegrationIT -Dfailsafe.failIfNoSpecifiedTests=false

# 完整构建；deploy profile 用于正确处理 ${revision}
mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy
```

`mvn test` 只运行 Surefire 单元测试，`mvn verify` 同时运行 Failsafe 集成测试。验证从最小相关范围开始，再按改动风险扩大。涉及外部 Redis、Kafka、RocketMQ、TinyID、数据库或云服务的测试，先确认运行条件；不要把环境缺失误判为代码失败。

## 全局约束

- 保持 Java 17 兼容；包名沿用 `dev.macula...`。
- Java 排版由开发者编辑器负责；IDEA 自动使用版本化的 `.idea/codeStyles` 项目方案，`.editorconfig` 提供跨编辑器的基础行为。不要使用 Maven 自动重排源码。
- CI 对分支中变更的 Java 文件执行 Checkstyle；新增 Java 文件必须使用 `.config/license-header` 定义的 Apache License 2.0 版权头。
- 类、接口、枚举、注解、Record 及测试类必须使用类级 Javadoc 说明职责；顶层类型必须包含非空的 `@author`、`@since`，内部类型无需重复 `@author`，但仍须包含 `@since`，格式参考 `AsyncAutoConfiguration`。公开 API 还应按需说明参数、返回值、异常和重要约束，禁止用注释逐行翻译代码。
- Starter 自动配置应可覆盖、可禁用，并避免无条件创建 Bean。
- 新增或移动自动配置时，同步检查 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 行为、配置键或公共 API 变化时，同步更新目标模块 README 和测试。
- 根 README 是项目概览、构建和测试规范的统一入口；避免创建内容重复的根级说明文档。
- 不提交生成物、凭据、密钥或本地环境配置。
- 不主动执行 `release.sh`、发布、打 tag 或推送；只有用户明确要求发布时才进行。

## 按需加载规则

只读取与当前任务相关的规则：

- 理解模块边界或跨模块修改：`.agents/rules/architecture.md`
- 修改/新增 Starter、自动配置或配置属性：`.agents/rules/starter-development.md`
- 编写、重命名测试或选择验证范围：`.agents/rules/testing.md`
- 修改 POM、依赖版本、CI 或执行发布：`.agents/rules/dependencies-release.md`
