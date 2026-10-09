# Jackson 3 与 JSpecify 后续变更验证

日期：2026-10-09
分支：`fix/web-message-converters-builder`（未提交工作区）

## 范围与结论

验证用户本轮确认的 Jackson 3 自有源码迁移、MVC ServerBuilder 回调、SSE/UTF-8、基本类型 null 兼容、转换器重命名及 Nullable 统一为 JSpecify。

全仓启用的单元测试和集成测试通过，变更 Java 文件 Checkstyle 通过。真实 RocketMQ、Kafka、TinyID 测试已有 `@Disabled`，本轮未解除，不能据此声称这些服务的端到端兼容性已验证。

## 实际执行结果

`mvn clean verify -Dstyle.color=never`，退出码 0：

```text
[INFO] BUILD SUCCESS
[INFO] Total time:  01:28 min
[INFO] Finished at: 2026-10-09T10:42:58+08:00
```

从本次 clean 构建生成的 Surefire/Failsafe 报告汇总：

```text
surefire: tests=206 failures=0 errors=0 skipped=0
failsafe: tests=49 failures=0 errors=0 skipped=3
```

跳过项：`KafkaProducerIT`、`OrderServiceIT`、`TinyIdClientIT`，各 1 项，均为仓库既有外部服务禁用项。Redis 端口检查通过，相关已启用集成测试已实际执行。

`mvn -N checkstyle:check -Dcheckstyle.includes=<git diff HEAD --name-only --diff-filter=ACMR 中全部 Java 文件，逗号分隔> -Dstyle.color=never`，退出码 0，按当前仓库 CI 的变更文件范围执行：

```text
[INFO] You have 0 Checkstyle violations.
[INFO] BUILD SUCCESS
```

`git diff --check` 退出码 0。维护的 Java 源码（含测试、示例、archetype，不含 target）未检出 Jackson 2 core/databind/datatype/module/dataformat 引用或旧 Spring Jackson 2 转换器引用；Nullable 导入全部为 JSpecify。共享 `com.fasterxml.jackson.annotation` 注解及第三方 Jackson 2 依赖继续保留。

日志：`/tmp/macula-jackson-clean-verify-20261009.log`、`/tmp/macula-jackson-checkstyle-20261009.log`。

## 与计划的对应关系

- Web 回归覆盖 ServerBuilder 注册、UTF-8 响应头与中文字节、SSE 行前缀、基本类型 null 兼容及自动配置开关；完整 Web 上下文测试通过。
- RocketMQ 无 Broker 回归覆盖 Jackson 3 转换链、历史 JSON、Java 时间类型、长整数精度、字符串与字节数组及未知字段/null 兼容。真实 Broker 收发未验证。
- 全仓 clean 构建覆盖重命名及 JSpecify 注解编译、现有模块单元/集成测试。
- 本轮未修改 POM、revision、deploy Profile、CI、Compose 或 archetype 模板；历史平台升级的分支创建、发布构建、镜像部署与云服务冒烟不在本轮增量验证范围，历史报告不作为当前运行证据。
- 未修改 AGENTS.md、CLAUDE.md、技能或 hook；配置 eval 不适用。
- 尚未执行 Stage 5 独立审查，未提交、推送、创建 PR 或发布。

## 验证边界

本报告证明仓库已启用的自动化检查通过，不宣称真实 RocketMQ 端到端验证完成。若将真实 Broker 收发列为此次发布必需证据，应补齐对应环境和验证后再将完整验证标绿。

## 构建 Warning 修复复验（2026-10-09 10:49）

已修复：Crypto 使用 Boot 4 `EnvironmentPostProcessor` 并同步 `spring.factories` 注册 key；Security 使用 `getPatternValues()` 消除路径匹配 API 弃用；Archetype 显式声明 UTF-8 编码。Cache 测试继续模拟上游实际调用的 `clean()`，仅在该测试方法抑制 removal 警告并记录原因，避免更换 mock 方法后丢失故障覆盖。

重新执行 `mvn clean verify -Dstyle.color=never`，退出码 0：

```text
[INFO] BUILD SUCCESS
[INFO] Total time:  01:27 min
[INFO] Finished at: 2026-10-09T10:49:59+08:00
surefire: tests=206 failures=0 errors=0 skipped=0
failsafe: tests=49 failures=0 errors=0 skipped=3
```

全部变更 Java 文件 Checkstyle 重新执行通过，0 违规；`git diff --check` 通过。日志：`/tmp/macula-warning-fixes-20261009.log`、`/tmp/macula-warning-checkstyle-20261009.log`。

剩余 Maven Warning：71 条无 Mapper 模块不识别全局 `mapstruct.defaultComponentModel` 参数、6 条空 JAR、1 条缺少 Archetype IT 项目、3 条既有外部服务测试跳过。运行期仍有 Netty macOS DNS native resolver 缺失、Fenix/Springdoc 配置提示及缓存故障模拟日志。

后续处理建议：MapStruct 参数改为业务模块按需启用，须同时迁移现有 Mapper、骨架及下游默认 Spring Bean 约定；依赖聚合型空 JAR 模块评估改为 pom 包装，空示例评估是否补齐实现；Archetype 增加真实生成/编译 IT；macOS 应用或本地验证 profile 按 CPU 架构补充受 BOM 管理的 `netty-resolver-dns-native-macos`。这些措施涉及公共构建语义或额外依赖，未为消除日志而直接更改。故障模拟、启用状态和测试跳过日志保留如实报告。
