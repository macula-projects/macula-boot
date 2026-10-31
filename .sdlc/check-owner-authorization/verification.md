# Verification: CheckOwner

Date: 2026-11-01
Status: passed

## Accepted baseline
- intent.md、spec.md、plan.md 均已得到用户明确接受。
- 用户确认实现完成后进入 Test；源码仅变更 Security Starter，未新增配置类。

## Commands and actual output

1. `mvn -pl macula-boot-starters/macula-boot-starter-security -am test -Dtest=CheckOwnerTest,SecurityAutoConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false`，最终退出码 0。

```text
[INFO] Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

2. `mvn -pl macula-boot-starters/macula-boot-starter-security -am test`，退出码 0；Commons 24 个、Security 22 个，共 46 个。

```text
[INFO] Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
[INFO] Tests run: 22, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

3. `mvn -N checkstyle:check '-Dcheckstyle.includes=**/security/annotation/CheckOwner.java,**/security/config/SecurityAutoConfiguration.java,**/security/test/CheckOwnerTest.java,**/security/test/SecurityAutoConfigurationTest.java'`，退出码 0。

```text
[INFO] You have 0 Checkstyle violations.
[INFO] BUILD SUCCESS
```

4. `mvn test`，最终退出码 0；汇总本次日志中 26 个模块的 Surefire 结果：230 个测试，失败 0，错误 0，跳过 0。

```text
[INFO] BUILD SUCCESS
[INFO] Total time:  20.169 s
```

5. `git diff --check`，退出码 0，无输出。

## Failure history and resolution
- 首次定向测试：方法鉴权 10 个通过；自动配置 2 个失败，原因是测试缺少现有 SecurityProperties 初始化所需的 SpringUtil/MVC 依赖。补齐测试上下文，改用真实 RequestMappingHandlerMapping，删除与容器请求依赖重复的 mock 后，12 个全部通过。生产鉴权逻辑无需修正。
- 首次全仓运行：未变更的 MapStruct 模块报 `cannot find converter from User to UserDto`。定向 clean test 时发现注解处理器写入 `/Users/Rain/.msp/incrementMark` 受执行权限限制；获执行权限后干净构建通过（2 个测试），重新运行全仓通过。未修改 MapStruct 源码。

## Proof coverage
- 必填业务表达式：直接对象、不同业务属性、嵌套包装与安全导航均通过真实代理验证。
- 拒绝路径：归属不匹配、缺少身份、匿名、未认证、空返回、空拥有者、空嵌套数据均验证；前置拒绝的方法体未执行。
- 无 ADMIN/ROOT 隐式绕过；错误属性不返回数据；未标注方法行为不变。
- 现有 Starter 自动配置真实装配模板并执行归属检查；应用提供同类型 Bean 时默认 Bean 回退，且模板仍正常工作。
- README 说明身份标识、代理、ResponseBodyAdvice、只读/列表/异步/租户边界；未修改 Result、网关、数据库、依赖或自动配置 imports。
- Java 17 生产与测试编译通过；规范检查覆盖全部 4 个变更 Java 文件。

## Configuration and evals
只修改 Starter 生产配置及测试、文档和本需求记录。未修改 AGENTS.md、CLAUDE.md、skill、hook、REVIEW.md 或 reviewer 定义，无适用的配置 eval 变更。

## Limits
本次运行 Surefire 单元测试，不包含 Failsafe 集成测试；本功能无需外部服务。端到端部署及生产环境未验证。

Verification is green
