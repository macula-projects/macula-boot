# Starter 开发规则

修改自动配置、配置属性或新增 Starter 时加载。

## 自动配置

- 使用 Spring Boot 3 的 `@AutoConfiguration` 与条件注解，避免组件扫描产生隐式副作用。
- 面向用户可替换的 Bean 默认使用 `@ConditionalOnMissingBean`；按需组合 `@ConditionalOnClass`、`@ConditionalOnProperty`、`@ConditionalOnWebApplication`。
- 新增自动配置类时登记到 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`，并检查配置顺序需求。
- 兼容入口仅在确有历史兼容需求时保留 `spring.factories`，不要把它作为 Boot 3 自动配置的默认登记方式。
- 配置属性使用稳定前缀和 `@ConfigurationProperties`；新增键应有合理默认值，并在模块 README 中说明。

## 依赖与 API

- Starter POM 只声明运行所需依赖；可选集成不要强制污染使用方 classpath。
- 依赖版本优先放入 `../../macula-boot-parent/pom.xml`，子模块通过 dependency management 引用。
- 不泄漏第三方库的内部类型到公共 API，除非该类型本就是集成契约的一部分。
- 保持默认行为向后兼容；改变默认值、Bean 名、配置键或序列化行为时补充回归测试和迁移说明。

## 最低验证

- 覆盖自动配置启用、禁用及用户自定义 Bean 覆盖场景。
- 如果改动依赖 classpath 条件，至少验证依赖存在和缺失两条路径。
- 同步检查模块 README、测试配置及 `AutoConfiguration.imports`。
