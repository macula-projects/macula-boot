# 测试与验证

编写、重命名测试或决定验证范围时加载。根 `README.md` 是面向开发者的测试规范入口，本文件补充智能体执行约束。

## 测试分层

### 单元测试

- 命名为 `*Test`、`*Tests`、`Test*` 或 `*TestCase`，由 Maven Surefire 在 `test` 阶段执行。
- 纯逻辑优先使用 JUnit 和 Mockito，不启动 Spring。
- Starter 的条件装配、属性绑定和 Bean 回退优先使用 `ApplicationContextRunner` 或 `WebApplicationContextRunner`。
- 单元测试不使用 `@SpringBootTest`，也不连接 Redis、数据库、消息队列、HTTP 服务或云平台。

### 集成测试

- 命名为 `*IT`、`IT*` 或 `*ITCase`，由 Maven Failsafe 在 `integration-test` 和 `verify` 阶段执行。
- 完整 Spring 应用上下文、自动配置发现和 `@SpringBootTest` 测试属于集成测试。
- 即使没有使用 `@SpringBootTest`，只要连接 Redis、数据库、Kafka、RocketMQ、TinyID 或其他外部服务，也属于集成测试。
- 仅含 `main` 方法的手工诊断程序使用 `*Manual` 等不会被 Surefire/Failsafe 识别的名称。

## 测试策略

- 新增测试文件必须包含仓库标准 Apache License 2.0 版权头；测试类必须包含说明测试对象的类级 Javadoc，顶层测试类型必须包含非空的 `@author`、`@since`，内部类型无需重复 `@author`，但仍须包含 `@since`。
- 修复缺陷先添加能复现问题的测试；新行为同时覆盖正常路径和关键边界。
- 自动配置测试至少考虑默认路径、属性开关、条件不满足和用户 Bean 回退；核心 Starter 可另加少量 `*IT` 验证 `AutoConfiguration.imports` 发现链路。
- 沿用目标模块现有的 JUnit、Mockito、Spring Test 与断言风格，不为单个改动引入新的测试框架。
- 测试应可重复、与执行顺序无关；避免真实时间、随机值、网络或共享外部状态造成不稳定。
- 不捕获异常后只打印堆栈；失败必须通过断言或未处理异常反馈给测试框架。

## 验证顺序

1. 单个单元测试：`mvn -pl <module-path> -am test -Dtest=TestClass#method -Dsurefire.failIfNoSpecifiedTests=false`。
2. 目标模块单元测试：`mvn -pl <module-path> -am test`。
3. 指定集成测试：`mvn -pl <module-path> -am verify -Dit.test=IntegrationIT -Dfailsafe.failIfNoSpecifiedTests=false`。
4. 跨模块代码修改后运行 `mvn test`；涉及集成边界、父 POM、测试插件或 CI 时运行 `mvn clean verify`。
5. 影响 deploy Profile、flatten、revision 或发布配置时，再运行 `mvn clean install -DskipTests=true -Dgpg.skip=true -Pdeploy`。

## 环境依赖

- Redis 集成测试要求 Redis 可用；仓库 CI 使用 Redis 7。运行前查看测试资源和模块 README，不假设本地服务一定存在。
- Kafka、RocketMQ、TinyID、数据库和云服务测试必须明确地址、凭据及准备步骤，禁止提交真实凭据。
- 可选基础设施暂未在 CI 提供时，允许使用带明确原因的 `@Disabled`，但应同时保留无需外部服务的配置或逻辑测试。
- 无法满足外部条件时，报告未运行项、所需条件和已完成的替代验证，不要静默跳过，也不要把环境缺失误判为代码失败。

## 结果报告

- Surefire 报告位于各模块的 `target/surefire-reports`，Failsafe 报告位于 `target/failsafe-reports`。
- 最终说明实际执行的命令、测试数、失败/错误/跳过数，以及任何未验证范围。
