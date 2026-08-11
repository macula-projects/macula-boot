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

# 完整构建；deploy profile 用于正确处理 ${revision}
mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy
```

验证从最小相关范围开始，再按改动风险扩大。涉及外部 Redis、Kafka、RocketMQ、数据库或云服务的测试，先确认运行条件；不要把环境缺失误判为代码失败。

## 全局约束

- 保持 Java 17 兼容；包名沿用 `dev.macula...`。
- Starter 自动配置应可覆盖、可禁用，并避免无条件创建 Bean。
- 新增或移动自动配置时，同步检查 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。
- 行为、配置键或公共 API 变化时，同步更新目标模块 README 和测试。
- 不提交生成物、凭据、密钥或本地环境配置。
- 不主动执行 `release.sh`、发布、打 tag 或推送；只有用户明确要求发布时才进行。

## 按需加载规则

只读取与当前任务相关的规则：

- 理解模块边界或跨模块修改：`.agents/rules/architecture.md`
- 修改/新增 Starter、自动配置或配置属性：`.agents/rules/starter-development.md`
- 编写测试或选择验证范围：`.agents/rules/testing.md`
- 修改 POM、依赖版本、CI 或执行发布：`.agents/rules/dependencies-release.md`
