# Spec: 任务调度能力整理与 SnailJob 统一执行 (from intent.md 2026-09-27)
Status: accepted

## Source intent
[任务调度能力整理与 SnailJob 统一执行](./intent.md)：task Starter 兼容 XXL-JOB 与 SnailJob 两种客户端代码，但统一由 SnailJob Server 调度和执行，不再需要 XXL-JOB Admin。

## Governing policies
- 根目录 `AGENTS.md`：保持 Java 17、Spring Boot 4、Starter 可覆盖/可禁用，配置、README 和测试同步，并遵守 AI-Native SDLC 门禁。
- `.agents/rules/architecture.md`：兼容适配留在 task Starter；example 只作为消费者验证，不成为框架依赖来源。
- `.agents/rules/starter-development.md`：自动配置必须有 classpath/property 条件，默认 Bean 可替换，新增配置键有稳定默认值，并覆盖启用、禁用和回退测试。
- `.agents/rules/testing.md`：使用 `ApplicationContextRunner` 和纯单元测试验证适配，不把真实 SnailJob Server 作为单元测试前提。
- `.agents/rules/dependencies-release.md`：第三方版本由 `macula-boot-parent` 集中管理，POM 变化至少验证目标模块及依赖模块。
- 根目录 `REVIEW.md`：实现后分别执行 Bugs、Security、Compliance 审查；第三方依赖、反射调用和任务参数属于重点检查面。
- 根目录 `bands.yaml`：本次不修改运行指标、SLO 或控制带。

## Requirements
1. 保持 `snail-job.version=1.9.0`；所有产物必须在 Java 17 下编译和运行，不升级到基于 Java 21 class format 的 SnailJob 2.0.0。
2. 业务应用只声明 `macula-boot-starter-task` 后，必须获得 SnailJob starter、Job、Retry API，以及运行旧 Handler 所需的最小 XXL 兼容 API；不再直接或传递依赖官方 `xxl-job-core`。
3. task Starter 必须以 SnailJob Server 作为唯一调度服务端；产物中不得包含 `XxlJobSpringExecutor`、XXL 网络通信、Admin 回调和日志拉取实现，应用不需要 `xxl.job.admin.*` 或 XXL-JOB Admin。
4. 业务应用引入 task Starter 后必须默认获得与显式添加 `@EnableSnailJob` 相同的启用效果，不需要在启动类增加该注解，也不需要手工设置 `snail-job.enabled=true`；默认值必须在 SnailJob 自动配置条件求值前生效。显式配置 `snail-job.enabled=false` 时，SnailJob 客户端和 XXL 兼容适配均不得启动。
5. SnailJob 原生 `@JobExecutor`、`JobArgs`、`ExecuteResult` 和 `@Retryable` 客户端代码必须按 1.9.0 原生语义继续工作。
6. Starter 必须扫描 Spring Bean 上的 `@XxlJob` 方法，并以注解的 `value` 作为 SnailJob executor name 注册到 SnailJob 1.9 客户端，使原任务方法无需增加或替换注解。
7. XXL 兼容执行器必须把 SnailJob `JobArgs` 映射为当前线程的 `XxlJobContext`：`jobParams` 映射到字符串任务参数，`jobId` 映射任务 ID，`taskBatchId` 映射日志 ID，分片任务的 index/total 映射到 XXL 分片上下文，非分片任务使用 `0/1`。
8. 非字符串 `jobParams` 必须序列化为 JSON 字符串，空参数保持为空；每次执行结束后必须清理 `XxlJobContext`，防止线程池复用导致上下文串任务。
9. XXL Handler 正常返回或调用 `XxlJobHelper.handleSuccess` 时必须转成 SnailJob 成功结果；调用 `handleFail`、`handleTimeout` 或抛出异常时必须转成 SnailJob 失败结果，并保留可用的失败消息。
10. `XxlJobHelper.log(...)` 必须仍可调用并输出日志；README 必须说明通过 SnailJob 官方 Logback appender 将 SLF4J 日志上报到 SnailJob Server，而不是依赖 XXL-JOB Admin 拉取本地日志文件。
11. `@XxlJob(init=..., destroy=...)` 生命周期必须保留：注册兼容执行器时调用 init，Spring 上下文销毁时调用 destroy；初始化失败必须阻止错误的执行器注册。
12. XXL Handler 名称为空、同一应用中重复，或与 SnailJob 原生 executor name 冲突时必须启动失败并给出明确冲突信息，不允许静默覆盖。
13. XXL Handler 方法遵循既有 Bean Handler 调用兼容性：支持无参数方法；引用类型参数按原行为传入 `null`；包含 primitive 参数的方法必须在启动扫描时明确拒绝。
14. XXL-to-Snail 兼容适配默认开启，并提供 `macula.task.xxl-job-adapter.enabled=false` 关闭开关；关闭后 SnailJob 原生 Handler 仍可运行。
15. `macula-example-task` 必须删除对 XXL-JOB 和 SnailJob 的重复直接依赖，移除 `@EnableSnailJob`，仅用 task Starter 且无需显式配置 `snail-job.enabled=true` 即可完成装配；保留原 XXL Handler 代码，通过 SnailJob 执行，并保留一个不重名的 SnailJob 原生 Handler 示例。
16. task Starter README 必须删除 PowerJob、XXL-JOB Admin 和双服务端部署内容，完整说明单 SnailJob Server 架构、1.9.0 配置、两种 Handler 写法、上下文映射、名称冲突、日志与迁移限制。
17. 目标模块与 task example 中不得残留 PowerJob 依赖、配置、示例或许可证引用；不得修改无关的 `polaris/` 等用户文件。
18. 若应用显式或通过其他依赖再次引入官方 `xxl-job-core`，Starter 必须检测官方 executor 类型并启动失败，明确提示移除冲突依赖，避免同包同名类由 classpath 顺序随机覆盖。
19. task Starter 必须提供 `macula.task.enabled=false` 总开关；关闭后不得自动启用 SnailJob、注册兼容桥或建立任何调度服务连接。
20. SnailJob 1.9.0 运行时所需但其发布 POM 未传递的 Jackson 2 `databind` 与 `jsr310` 构件，必须由 task Starter 显式补齐并继续受 Spring Boot BOM 管理；该兼容路径仅服务于固定的 SnailJob 1.9 客户端，不得扩展为新的应用序列化方案。

## Non-goals
- 不部署、管理或兼容 XXL-JOB Admin；不支持由 XXL-JOB Server 触发本次适配后的任务。
- 不迁移 XXL-JOB Admin 中已有的任务定义、Cron、路由策略、执行历史、告警或用户数据；控制台任务需要在 SnailJob Server 中重新建立或导入。
- 不把 XXL-JOB 的 GLUE、脚本任务、命令行任务协议或 Admin OpenAPI 转换成 SnailJob 协议；只适配应用内 Spring Bean 模式的 `@XxlJob` Handler。
- 不承诺 XXL-JOB 与 SnailJob 所有高级调度策略一一等价；调度、阻塞、超时和重试策略以 SnailJob Server 配置为准。
- 不升级 SnailJob 或扩展统一 `macula.task` 领域 API。
- 不兼容 `xxl-job-core` 的完整 API；最小兼容面之外的 Executor、Admin、OpenAPI、GLUE、脚本和内部工具类型不予提供。
- 不要求真实 SnailJob Server 参与单元测试；服务端联调作为有环境时的集成验证。

## Design
### Dependency and activation
`macula-boot-parent` 保持 SnailJob `1.9.0`，删除仅供 task Starter 使用的 `xxl-job.version` 和 `xxl-job-core` dependency management。task Starter 完全删除 `xxl-job-core`，将 `snail-job-client-starter`、`snail-job-client-job-core`、`snail-job-client-retry-core` 改为可传递依赖，并在自身 jar 中提供最小 XXL 兼容 API。

删除旧的 `XxlJobConfiguration`、`XxlJobProperties`、`XxlAdminProperties` 和 `XxlExecutorProperties`。`TaskAutoConfiguration` 保留 `@EnableSnailJob` 以复用官方 AOP order、group 和注册语义；同时由 `TaskEnvironmentPostProcessor` 在自动配置选择前提供启用值：`macula.task.enabled=false` 时以最高优先级强制 `snail-job.enabled=false`，总开关开启时仅在用户未显式配置 SnailJob 开关的情况下，以最低优先级补充默认 `true`。因此 task 总开关优先于冲突的显式 SnailJob 启用值，而用户显式 `snail-job.enabled=false` 仍优先于默认启用。Macula 桥接配置同时受 task 总开关、`snail-job.enabled` 和适配器开关约束。

SnailJob 1.9.0 的 `JsonUtil` 直接链接 Jackson 2 `databind` 与 `jsr310` 类型，但其发布 POM 没有把这些运行时必需构件传递给消费者。task Starter 显式声明由现有 Spring Boot BOM 管理版本的 `jackson-databind` 与 `jackson-datatype-jsr310`，用于保证 SnailJob 对象任务参数 JSON 映射在 Java 17 / Boot 4 下可运行；Macula 应用侧仍遵循仓库既定的 Jackson 3 主路径。

### Minimal XXL compatibility API
为保持旧业务源码 import 不变，task Starter 使用原有公开包名提供独立实现的最小兼容类型：

- `com.xxl.job.core.handler.annotation.XxlJob`：保留 `value`、`init`、`destroy` 属性及运行时方法注解语义。
- `com.xxl.job.core.context.XxlJobContext`：保留成功、失败、超时状态常量，任务/日志/分片字段、handle 状态以及当前线程 context 的 getter/setter。
- `com.xxl.job.core.context.XxlJobHelper`：保留任务、日志、分片 getter，`log(String,Object...)`、`log(Throwable)`，以及 `handleSuccess`、`handleFail`、`handleTimeout`、`handleResult` 重载。

这些类必须使用 Macula 的 Apache License 2.0 文件头并独立实现，不复制或改编官方 XXL-JOB 的 GPL 源码。兼容 API 只依赖 SLF4J 和 JDK 类型。Starter 通过检测仅存在于官方客户端的 `com.xxl.job.core.executor.XxlJobExecutor` 判断 classpath 冲突并快速失败。

### XXL handler discovery
新增一个实现 SnailJob 1.9 `Scanner` SPI 的 Spring Bean。它参考 XXL-JOB 自身的 Spring 扫描语义，通过 `MethodIntrospector` 与 merged annotation 查找所有非懒加载 Spring Bean 的 `@XxlJob` 方法，并处理 Spring AOP 代理的可调用方法。

每个 XXL 方法被包装为独立桥接执行器对象，并生成 `JobExecutorInfo`：executor name 等于 `@XxlJob.value`，执行方法为桥接对象公开的 `execute(JobArgs)`，返回类型为 SnailJob `ExecuteResult`。SnailJob 的 `JobExecutorRegistrar` 在启动时把这些信息与原生 `@JobExecutor` 一起注册到同一个 Server。空名称、XXL 内部重复名或与原生 SnailJob 重名均直接失败。

### Execution bridge
桥接对象持有业务 Bean、执行方法以及可选 init/destroy 方法。收到 `JobArgs` 后：

1. 将 SnailJob 参数、任务 ID、批次 ID和分片信息转换成 `XxlJobContext` 并绑定当前线程。
2. 按既有 Bean Handler 的兼容语义调用原业务方法：无参数直接调用，引用类型参数传 `null`。
3. 读取 `XxlJobContext.handleCode/handleMsg`，把 200 转为 `ExecuteResult.success`，把 500、502 和其他非成功码转为 `ExecuteResult.failure`。
4. 解包反射调用异常，把真实业务异常交给 SnailJob 失败上报。
5. 在 `finally` 中将 ThreadLocal context 设为 `null`。

`XxlJobHelper.log` 使用空的 XXL log file name，使其回落到 `xxl-job logger` 的 SLF4J 输出；接入 README 中的 SnailJob Logback appender 后，该日志跟随当前 SnailJob 日志上下文远程上报。

### Lifecycle and examples
扫描器在生成 bridge 前解析并调用 `@XxlJob.init`；成功初始化的 bridge 由扫描器记录，在 Spring Bean 销毁阶段按逆序调用 `destroy`。初始化失败时不注册该 Handler，并让应用启动失败。

example 删除官方 XXL-JOB 与三个 SnailJob 客户端直依赖，并从 `MaculaExampleTaskApplication` 移除 `@EnableSnailJob`；配置文件只保留 SnailJob Server、namespace、group、token、host、port 等连接参数，以证明“引入 task Starter 即自动启用”的契约。现有 `XxlJobDemoHandler` 的 import、注解和方法体保持不改，由 task Starter 自带的兼容 API 编译；当前与它重名的 SnailJob `demoJobHandler` 改为唯一名称。

## Data and interfaces
- Maven 契约：task Starter 的消费者传递获得 SnailJob 1.9.0 客户端及其缺失的 Jackson 2 运行时兼容构件；XXL 兼容类型直接包含在 task Starter jar 中，不再出现官方 `xxl-job-core`。
- Java 兼容契约：仅保证 `@XxlJob`、`XxlJobContext`、`XxlJobHelper` 上述最小公开方法的源码和二进制链接兼容。
- 服务端契约：只连接 `snail-job.server.host/port` 指向的 SnailJob Server；不读取或连接 XXL-JOB Admin。
- 启用契约：引入 task Starter 默认自动启用 SnailJob；`macula.task.enabled=false` 关闭整个 Starter；显式 `snail-job.enabled=false` 关闭 SnailJob 客户端及桥接；`macula.task.xxl-job-adapter.enabled=false` 只关闭 XXL Handler 桥接。三个开关均以显式关闭优先。
- Executor 命名契约：`@XxlJob.value` 与 `@JobExecutor.name` 共享同一 SnailJob executor 命名空间，必须全局唯一。
- 参数契约：SnailJob `jobParams` 转为 `XxlJobHelper.getJobParam()` 字符串；对象参数使用 JSON。
- 分片契约：SnailJob sharding index/total 映射到 `XxlJobHelper.getShardIndex/getShardTotal`。
- 结果契约：XXL handle code 200 为 SnailJob 成功，其他 handle code 或异常为失败。
- 日志契约：XXL Helper 日志进入 SLF4J/SnailJob appender，不提供 XXL Admin 的本地日志拉取接口。
- 持久化与 Schema：应用侧无新增表；SnailJob Server 数据结构不在本次修改范围。

## Flagged concerns
- 使用原 XXL 包名提供最小兼容类型会形成有意的同包兼容层：必须采用独立实现并保留 Macula Apache 2.0 版权头，严禁复制官方 GPL 源码；实现与审查需特别核对来源，blocking。
- 最小兼容面不等于完整 `xxl-job-core`：若下游代码还直接使用 `XxlJobExecutor`、`IJobHandler`、`XxlJobFileAppender` 或其他内部类型，将无法编译，需要先盘点并另行决定是否扩展兼容面，blocking。
- SnailJob 1.9 的 `Scanner`、`JobExecutorInfo`、`JobExecutorRegistrar` 是公开类型但属于客户端内部扩展面：桥接会与 1.9.0 的注册结构耦合，因此本次固定版本并用契约测试保护，未来升级 SnailJob 必须重新验证，non-blocking。
- `@XxlJob` 与原生 `@JobExecutor` 共享 executor name：现有 example 已出现 `demoJobHandler` 重名，必须改名 SnailJob 示例；真实业务迁移前也必须消除冲突，blocking。
- XXL-JOB 调度策略不能全部由 Handler 代码表达：应用代码可保持不变，但 Cron、路由、阻塞、超时、重试等控制台配置必须在 SnailJob 中重建，non-blocking。
- `XxlJobHelper.log` 的底层不再写供 XXL Admin 拉取的日志文件：通过 SnailJob appender 可保留远程日志可见性，但依赖 `log(...)` 布尔返回值为 `true` 的非常规代码可能观察到差异，non-blocking。
- `XxlJobContext.logId` 只能由 SnailJob `taskBatchId` 近似映射，二者业务含义不完全相同；它保证单次批次可追踪，但不保证与旧 XXL 日志 ID 连续或可关联，non-blocking。
- XXL Handler 使用异步子线程时，`InheritableThreadLocal` 会复制上下文；父执行线程结束后的子线程生命周期不受桥接器控制，与 XXL-JOB 原有限制相同，non-blocking。
- 示例中的通用命令执行 Handler 具有命令注入风险，只能用于受信任本地演示，README 必须继续明确禁止生产开放，non-blocking。
- token 属于敏感配置：README 和示例只能使用环境变量及明显占位值，不得提交真实凭据，non-blocking。
- 引入 task Starter 将默认产生启动 SnailJob 客户端、监听通信端口和连接 Server 的运行副作用：这是用户要求的开箱即用行为，但非调度场景必须通过 `macula.task.enabled=false` 或 `snail-job.enabled=false` 显式关闭，non-blocking。

## Verification strategy
1. 用纯单元测试构造带 `@XxlJob` 的测试 Bean，验证扫描、名称、无参数调用、引用参数传 null、primitive 参数拒绝、init/destroy 和重复名称失败。
2. 直接调用 bridge，验证字符串/对象/空参数、普通/分片上下文、jobId/taskBatchId 映射、成功、`handleFail`、`handleTimeout`、异常解包以及执行后 ThreadLocal 清理。
3. 使用 `ApplicationContextRunner` 验证仅加载 task Starter 即产生与 `@EnableSnailJob` 相同的官方 Job/Retry Bean；验证 `macula.task.enabled=false` 不启用任何调度 Bean，显式 `snail-job.enabled=false` 覆盖默认启用，适配器单独关闭时原生 SnailJob Bean 仍存在，且 classpath 中不存在 `XxlJobSpringExecutor`。
4. 保留 SnailJob 官方注册器的集成契约测试：同时放入一个原生 `@JobExecutor` 和一个 `@XxlJob`，验证生成的 executor name 唯一且均进入注册缓存；测试不得访问真实 Server。
5. 运行 `mvn -pl macula-boot-starters/macula-boot-starter-task -am test` 并记录测试统计。
6. 运行 `mvn -pl macula-boot-examples/macula-example-task -am test`，证明 example 删除官方 XXL-JOB、SnailJob 直依赖、`@EnableSnailJob` 和 `snail-job.enabled=true` 后仍可编译并完成配置装配。
7. 检查 task Starter 和 example 依赖树，确认不存在 `com.xuxueli:xxl-job-core`，三个 SnailJob 客户端构件可传递且版本为 1.9.0；用 class major version 61 验证 Java 17 兼容。
8. 增加兼容 API 编译契约测试，使用未修改 import 的 `@XxlJob`/`XxlJobHelper` 示例编译并执行；另用测试 classloader 模拟官方 `XxlJobExecutor` 存在，验证冲突时快速失败。
9. 使用 `rg -n -i "power.?job|xxl.job.admin|com.xuxueli"` 检查目标模块、example、父 POM 和 README，确认 PowerJob、XXL Admin 配置及官方 XXL 依赖残留清零。
10. 对变更 Java 文件运行 Checkstyle，并按父 POM/跨模块改动风险扩大到 `mvn test`；如全仓失败，区分本次回归、既有失败和外部环境缺失。
11. 有可用 SnailJob 1.9 Server 时补充人工联调：分别触发原生 Snail Handler 和未改动的 XXL Handler，核对参数、分片、日志、成功/失败状态；无环境时明确报告未验证项。
