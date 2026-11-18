# Plan: 网关跨域认证响应与端点处理修正 (from spec.md 2026-11-18)
Status: accepted

## Files that change
以下路径以仓库根目录为基准：
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/security/ResourceServerConfiguration.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/filter/ProtectUrlsEndpointFilter.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/security/CorsSecurityWebFilterChainTest.java`（新增）
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/filter/ProtectUrlsEndpointFilterTest.java`（新增）
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/README.md`
- `macula-boot-examples/macula-example-alibaba-gateway/src/main/resources/application.yml`
- `macula-boot-examples/macula-example-alibaba-gateway/README.md`
- `macula-boot-examples/macula-example-alibaba-gateway/src/main/java/dev/macula/example/gateway/config/RedisConfiguration.java`（用户补充，发布前核对修正）
- `macula-boot-examples/macula-example-alibaba-gateway/src/test/java/dev/macula/example/gateway/config/RedisConfigurationTest.java`（新增绑定和装配回归）
- `.sdlc/gateway-cors-security-order/plan.md`
- `.sdlc/gateway-cors-security-order/review-findings.md`
- `.sdlc/gateway-cors-security-order/verification.md`

## Order of work
1. 保留 ResourceServerConfiguration.java 现有用户差异，以工作区内容为基线；先补能复现认证失败跨域响应缺头、特殊端点拒绝后仍生成正文的定向测试。
2. 使用 Spring Security Customizer.withDefaults() 启用 CORS，复用现有配置源；保留 CSRF 禁用并解释显式凭据边界。检查已有条件和返回 null 的配置源在无配置、空配置时不会导致启动失败；仅在必要时调整同文件内配置源装配，禁止引入默认通配放行。
3. ProtectUrlsEndpointFilter 在读配置及构造正文前校验 CORS；process 返回 false 时 setComplete，预检通过时 setComplete，无配置源的预检也不输出业务正文。同源实际请求与其他路径保持既有行为。
4. Alibaba 示例改为 spec 中四个本地 Origin，关闭 allow-credentials，保留现有允许方法和请求头规则；README 同步解释生产替换来源、签名请求头、Security 与路由层共用配置及 Cookie/CSRF 边界。
5. 完成类、方法 Javadoc 与新增文件版权头。Build 阶段运行目标模块及依赖 test-compile 和 git diff --check，核对用户原有差异、配置及新增测试覆盖并记录偏差；实现确认后交由 Test 阶段执行回归。

## Risks
- Security 提前执行 CORS 会改变非法来源请求的拒绝时机；这是预期边界，合法来源必须保留原认证错误状态与正文。
- 配置存在但映射为空与完全未配置均不能导致启动失败或新增开放来源；特殊端点没有配置源时仅维持既有实际请求行为。
- 特殊端点早于 Security，因此自身跨域校验不能删除；重复处理必须不产生重复响应头。
- 示例跨域来源收窄影响其他本地开发端口，README 要说明显式添加实际前端 Origin；同源加解密演示应继续可用。
- 不将用户默认 URL 权限检查、内省器 assert 或排版改动回退；Query Token、API Key、HMAC 和其他过滤器行为保持原样。

## Proof
- CorsSecurityWebFilterChainTest：使用实际 ResourceServerConfiguration 构建 SecurityWebFilterChain，用 mock 内省器、Redis 与内存 WebFlux 测试基础设施，覆盖允许来源的成功/无效 Token/无凭据拒绝，断言状态、错误正文与跨域头；拒绝来源、非法预检方法/请求头，合法预检不调用内省器/业务处理；验证无配置、空配置时装配及同源认证正常。使用同一配置源模拟后续 HandlerMapping CORS 处理，断言头不重复；明确该测试不等同真实 Gateway 路由集成联调。
- ProtectUrlsEndpointFilterTest：模拟 exchange，两个特殊端点各覆盖同源正文、允许来源、拒绝来源、合法/非法预检、无配置源预检；断言拒绝或预检不调用配置/公钥读取，不写业务正文，其他路径继续过滤链。
- 新增定向测试使用现有 JUnit、Mockito、Spring Test，不引入依赖或连接外部服务。定向命令：`mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dtest=CorsSecurityWebFilterChainTest,ProtectUrlsEndpointFilterTest,CorsConditionTest -Dsurefire.failIfNoSpecifiedTests=false`。
- 目标回归：`mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test`。实际执行、数量、失败与跳过记录在 verification.md；不将 test-compile 作为单测通过证据，不声称已完成真实浏览器、配置中心或外部服务联调。
- 静态检查示例四个 Origin、凭据关闭和文档一致性，运行 git diff --check；依据用户初始 diff 核对既有修改仍在。

## Implementation record
- 用户以 YES 明确接受 spec，随后以 YES 并引用 ProtectUrlsEndpointFilter.java 明确接受本计划，已进入 Build。
- 已新增两组回归测试、启用 Security CORS、修正特殊端点短路，收窄示例来源并同步两处 README。
- 保留 defaultUrlRequireCheck=true、内省器 assert、用户导入/排版及 Query Token 兼容行为。
- JDK 17 下执行目标 Reactor test-compile 与 git diff --check，均通过；没有运行单元测试，不将编译成功作为行为验证。实际命令与日志见 verification.md。
- 无实施范围偏差。测试为保持现有契约区分两种 401：缺失凭据的自定义错误正文，以及 Resource Server 默认无效 Token 入口的 Bearer challenge/空正文；仅验证跨域头补齐，不统一错误响应。
- 用户随后明确回复 YES 确认实现完整，已进入 Test；首次定向测试暴露模拟响应完成问题，已在现有测试辅助方法中修正并重跑通过。目标模块及依赖 145 项通过，具体证据见 verification.md。

## Test outcome
- 实现确认后，定向 20 项、Gateway Reactor 145 项及全仓 526 项均通过，0 失败/错误/跳过；四个 Java 文件 Checkstyle 0 违规。Verification is green，详见 verification.md。尚未提交、推送或部署。

## Release scope update
- 用户实际启动发现缺少 sysRedisTemplate，随后自行补充 RedisConfiguration 与 system Redis 配置，并明确要求“可以了。推送发布”。这些用户修改随本次交付纳入范围；发布前修正其 spring.redis 旧前缀为现有 YAML 的 spring.data.redis，并显式限定两套属性注入，避免 Primary 选择导致 System 使用网关配置。补方法 Javadoc 及轻量绑定/装配测试，不修改真实凭据、不连接或迁移外部数据。
- 后续命令以包含示例配置的全仓 mvn test、改动 Java Checkstyle 为最新发布证据，CI mvn verify 后按既有流程发布 6.1.0-SNAPSHOT。用户此轮已明确授权提交、推送和发布；原不提交/推送/部署约束由此授权替代。不执行带 reset/clean 的 release.sh，不创建未指定版本的正式 Release/tag。
