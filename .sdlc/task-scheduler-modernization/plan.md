# Plan: 任务调度能力整理与 SnailJob 统一执行 (from spec.md 2026-09-27)
Status: accepted

## Files that change
- `.sdlc/task-scheduler-modernization/plan.md`：保存获批的实施契约；实施过程中如出现文件、顺序、风险或验证偏差，先记录到本文档再继续。
- `macula-boot-parent/pom.xml`：删除 `xxl-job.version` 与 `com.xuxueli:xxl-job-core` dependency management；保留 SnailJob `1.9.0` 及其三个客户端构件管理。
- `macula-boot-starters/macula-boot-starter-task/pom.xml`：删除官方 XXL-JOB 依赖；将 SnailJob starter、job-core、retry-core 改为消费者可传递依赖；不新增第三方库。
- `macula-boot-starters/macula-boot-starter-task/src/main/java/dev/macula/boot/starter/task/config/TaskProperties.java`：增加默认开启的 task 总开关和 XXL-to-Snail adapter 嵌套开关，形成 `macula.task.enabled` 与 `macula.task.xxl-job-adapter.enabled` 配置元数据。
- `macula-boot-starters/macula-boot-starter-task/src/main/java/dev/macula/boot/starter/task/config/TaskAutoConfiguration.java`：增加 task 总开关、SnailJob 1.9 自动配置顺序与 `@EnableSnailJob` 组合启用，导入兼容桥配置；显式 `snail-job.enabled=false` 仍覆盖默认启用。
- 删除 `macula-boot-starters/macula-boot-starter-task/src/main/java/dev/macula/boot/starter/task/config/xxljob/XxlAdminProperties.java`、`XxlExecutorProperties.java`、`XxlJobConfiguration.java`、`XxlJobProperties.java`：移除 XXL Admin、原生 Executor 和旧配置模型。
- 新增 `macula-boot-starters/macula-boot-starter-task/src/main/java/com/xxl/job/core/handler/annotation/XxlJob.java`：独立实现旧 Handler 需要的最小注解兼容 API。
- 新增 `macula-boot-starters/macula-boot-starter-task/src/main/java/com/xxl/job/core/context/XxlJobContext.java`：独立实现任务参数、日志标识、分片、结果状态和线程上下文兼容 API。
- 新增 `macula-boot-starters/macula-boot-starter-task/src/main/java/com/xxl/job/core/context/XxlJobHelper.java`：独立实现常用 getter、SLF4J 日志和 success/fail/timeout 结果 API；不复制官方 GPL 源码。
- 新增 `macula-boot-starters/macula-boot-starter-task/src/main/java/dev/macula/boot/starter/task/compat/xxljob/XxlJobSnailJobConfiguration.java`：按 task、SnailJob 与 adapter 开关条件装配扫描器和冲突检查器，并允许用户覆盖 Macula 默认 Bean。
- 新增 `macula-boot-starters/macula-boot-starter-task/src/main/java/dev/macula/boot/starter/task/compat/xxljob/XxlJobSnailJobScanner.java`：扫描 `@XxlJob` Spring Bean、校验签名和名称、管理 init/destroy 生命周期，并生成 SnailJob `JobExecutorInfo`。
- 新增 `macula-boot-starters/macula-boot-starter-task/src/main/java/dev/macula/boot/starter/task/compat/xxljob/XxlJobSnailJobExecutor.java`：把 `JobArgs` 映射到 `XxlJobContext`，调用旧 Handler，将 handle code/异常映射成 `ExecuteResult`，并在 finally 清理线程上下文。
- 新增 `macula-boot-starters/macula-boot-starter-task/src/main/java/dev/macula/boot/starter/task/compat/xxljob/XxlJobClientConflictDetector.java`：检测官方 `XxlJobExecutor` class resource，发现 `xxl-job-core` 混入时快速失败并提示删除冲突依赖。
- `macula-boot-starters/macula-boot-starter-task/src/test/java/dev/macula/boot/starter/task/config/TaskAutoConfigurationTest.java`：重写为自动启用、三个关闭开关、用户 Bean 覆盖、无官方 XXL 客户端和 SnailJob 原生能力保留测试。
- 新增 `macula-boot-starters/macula-boot-starter-task/src/test/java/dev/macula/boot/starter/task/compat/xxljob/XxlJobSnailJobExecutorTest.java`：覆盖参数、分片、ID、成功/失败/超时、异常解包、引用参数和 ThreadLocal 清理。
- 新增 `macula-boot-starters/macula-boot-starter-task/src/test/java/dev/macula/boot/starter/task/compat/xxljob/XxlJobSnailJobScannerTest.java`：覆盖扫描、空名/重名、primitive 参数、AOP 可调用方法、init/destroy 和原生 Snail executor 名冲突。
- 新增 `macula-boot-starters/macula-boot-starter-task/src/test/java/dev/macula/boot/starter/task/compat/xxljob/XxlJobClientConflictDetectorTest.java`：用可控 class resource lookup 验证无冲突和官方客户端冲突两条路径。
- 新增 `macula-boot-starters/macula-boot-starter-task/src/test/java/com/xxl/job/core/context/XxlJobCompatibilityApiTest.java`：锁定三个最小兼容类型的签名、Helper 行为、日志调用和上下文缺失默认值。
- `macula-boot-starters/macula-boot-starter-task/README.md`：完整改写为单 SnailJob Server 架构，说明自动启用/关闭、1.9.0 配置、原生与 XXL Handler、映射语义、名称冲突、日志、迁移边界、依赖冲突和许可证。
- `macula-boot-examples/macula-example-task/pom.xml`：删除官方 XXL-JOB 和三个 SnailJob 客户端直依赖，只保留 task Starter 提供调度 API。
- `macula-boot-examples/macula-example-task/src/main/java/dev/macula/example/task/MaculaExampleTaskApplication.java`：删除 `@EnableSnailJob` import/注解，并把类说明调整为 Starter 自动启用 SnailJob。
- `macula-boot-examples/macula-example-task/src/main/java/dev/macula/example/task/SnailJobHandler.java`：将原生 Snail executor 从冲突的 `demoJobHandler` 改为唯一名称；不修改 `XxlJobDemoHandler.java`，以证明旧 XXL 代码原样编译运行。
- `macula-boot-examples/macula-example-task/src/main/resources/application.yml`：删除全部 `xxl.job.*`/XXL Admin 配置；保留并校正 SnailJob 1.9 的 Server、namespace、group、token、host、port 配置，不显式设置 enabled。
- `macula-boot-examples/macula-example-task/README.md`：改为只要求 SnailJob Server，说明 Starter 自动启用、两种 Handler 都由 SnailJob 执行及生产安全边界。
- `macula-boot-examples/README.md`：把 task 示例说明和前置条件改为单 SnailJob Server，不再要求 XXL-JOB Admin。

## Order of work
1. 已从本地 `main` 当前门禁提交创建并切换到 `feat/task-scheduler-modernization`；实施开始前再次核对 accepted spec、分支、工作区与全仓 XXL/PowerJob/SnailJob 引用，保存当前依赖树和 Java 17 class-version 基线，确认 `polaris/` 等无关文件保持不动。
2. 先添加最小 `XxlJob`、`XxlJobContext`、`XxlJobHelper` 独立实现及 `XxlJobCompatibilityApiTest`，只依据已接受的公开签名编写 Apache 2.0 代码，不复制官方实现。
3. 编写执行桥测试并实现 `XxlJobSnailJobExecutor`，依次锁定参数 JSON 化、普通/分片上下文、引用参数、handle code、异常解包和 ThreadLocal 清理。
4. 编写扫描与冲突测试，实现 `XxlJobSnailJobScanner` 和 `XxlJobClientConflictDetector`；完成空名/重复名、primitive 参数、Spring AOP、init/destroy 和官方客户端混入的失败路径。
5. 新增桥接条件配置，更新 `TaskProperties` 与 `TaskAutoConfiguration`；通过自动配置顺序组合 `@EnableSnailJob`，验证仅引入 Starter 即启用，同时三个显式关闭开关均优先。
6. 删除旧 XXL 自动配置和属性类，从 task Starter/父 POM 移除官方依赖并取消 SnailJob optional；运行聚焦测试、dependency tree 和静态扫描，确认只剩最小兼容包及 SnailJob 客户端。
7. 更新 example POM、启动类、原生 Snail Handler 名称和 application.yml；保持 `XxlJobDemoHandler.java` 字节级不修改，通过编译证明旧代码只靠 task Starter 工作。
8. 改写 Starter/example 文档及 examples 总览，明确单 Server、自动启用、关闭方式、兼容范围、控制台任务迁移、日志和安全边界。
9. 按从小到大顺序执行单类测试、task Starter `-am test`、task example `-am test`、dependency tree、残留引用扫描、Checkstyle、`git diff --check` 与全仓 `mvn test`；对照 spec 和文件清单记录任何偏差。

## Risks
- 在 `com.xxl.job.*` 原包名下实现兼容类型具有版权与来源风险；所有代码必须从已接受的行为契约独立实现，使用仓库 Apache 2.0 头，不复制官方 GPL 源码或注释。
- 最小兼容面只覆盖 `@XxlJob`、`XxlJobContext`、`XxlJobHelper`；未盘点到的下游若直接使用 Executor、IJobHandler、FileAppender 或其他官方类型会编译失败。回滚方式是整体回退本变更，不重新引入双客户端隐式路径。
- `@EnableSnailJob` 通过自动配置组合后必须早于 SnailJob Job/Retry 条件求值；使用显式 auto-configuration ordering 并以无注解 example 和 context-runner 测试证明，避免属性设置过晚。
- 引入 task Starter 默认会监听客户端端口并连接 SnailJob Server；`macula.task.enabled=false` 和显式 `snail-job.enabled=false` 必须在配置解析早期生效，避免“关闭后仍连接”的副作用。
- SnailJob 1.9 Scanner/JobExecutorInfo 属于客户端内部扩展面；固定 1.9.0 并用注册契约测试保护，后续升级必须重新验证。
- 反射扫描可能触发懒加载 Bean、错误调用代理目标或重复执行 init；实现跳过 lazy Bean、选择代理可调用方法、先完整校验后初始化，并在销毁阶段只处理已成功初始化项。
- SnailJob 与 XXL Handler 共用名称空间，真实应用可能存在重名；必须在启动期失败，不允许覆盖，example 只改 Snail 名称以保留 XXL 代码。
- `XxlJobHelper.log` 改由 SLF4J/Snail appender 上报，日志文件名与旧 Admin 拉取语义不再存在；README 和测试只承诺可见日志，不伪造本地文件协议。
- 示例的命令执行 Handler 存在命令注入风险；保持原文件不改，但 README 必须标明仅限受信任本地演示，生产不得开放。
- 父 POM 可能包含其他任务正在修改的依赖项；只删除精确的 XXL property/management 块，不覆盖或回退无关变化。

## Proof
- `XxlJobCompatibilityApiTest` 证明旧 import 所需类型与方法可编译，Helper 在有/无 context 时返回稳定值，日志与结果 API 可用，且实现不依赖官方客户端类。
- `XxlJobSnailJobExecutorTest` 证明字符串/对象/空参数映射、jobId/taskBatchId、普通 `0/1` 与分片 index/total、默认成功、显式成功/失败/超时、真实业务异常、引用参数 null 以及 finally 清理。
- `XxlJobSnailJobScannerTest` 证明 Spring Bean 扫描、AOP 可调用方法、executor name、空名/重复名/primitive 参数失败、init 一次、destroy 逆序及与原生 Snail executor 重名失败。
- `XxlJobClientConflictDetectorTest` 证明 classpath 无官方 Executor 时正常、发现官方 Executor resource 时以可操作消息快速失败。
- `TaskAutoConfigurationTest` 证明仅加载 task 自动配置即产生 SnailJob Job/Retry 基础 Bean；task 总开关和显式 SnailJob 关闭不启动客户端/桥接；adapter 单独关闭不影响原生 SnailJob；用户桥接 Bean 可覆盖默认实现。
- `mvn -pl macula-boot-starters/macula-boot-starter-task -am test` 证明 Starter 与上游模块单元测试通过；`mvn -pl macula-boot-examples/macula-example-task -am test` 证明 example 在无直依赖、无 `@EnableSnailJob`、无显式 enabled 配置下仍可编译装配。
- dependency tree 证明目标模块和 example 不含 `com.xuxueli:xxl-job-core`，只传递 SnailJob 1.9.0 三个客户端构件；class major version 检查证明 SnailJob 仍为 Java 17。
- `rg` 证明目标生产 POM/配置/README 中无 PowerJob、XXL Admin 和 `com.xuxueli` 残留，同时允许最小 `com.xxl.job.*` 兼容包、示例 Handler 和迁移说明存在。
- 对比 `git diff -- macula-boot-examples/macula-example-task/src/main/java/dev/macula/example/task/XxlJobDemoHandler.java` 为空，证明旧 XXL Handler 源码未改。
- `mvn -N checkstyle:check`、`git diff --check` 和全仓 `mvn test` 验证 Java 规则、补丁完整性及跨模块回归；若外部环境或既有失败阻断，记录准确命令与证据，不误报成功。
