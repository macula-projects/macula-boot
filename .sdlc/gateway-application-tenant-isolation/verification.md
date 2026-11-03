# 网关应用租户隔离验证记录

日期：2026-11-03。Rain 已回复“OK，代码确认”，本轮使用 sdlc-test 执行验证。

## 结果

默认启用的全量测试与构建检查通过，失败及错误均为 0。存在按项目既有条件禁用的外部环境专项测试，不将跳过项计为通过；未宣告所有外部环境验证完成，也未进入 PR/发布阶段。

以下数值逐项汇总最终 verify 日志中的测试类报告，包含 Surefire 和 Failsafe，未重复计算各模块汇总行。

| 范围 | 总数 | 通过 | 失败/错误 | 跳过 |
| --- | ---: | ---: | ---: | ---: |
| Boot 全仓 verify | 327 | 324 | 0/0 | 3 |
| Cloud 全仓 verify | 405 | 395 | 0/0 | 10 |
| Admin 全量 Vitest | 63 | 63 | 0/0 | 0 |

## 实际命令及输出

Boot 根目录：

```sh
mvn -o -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test -Dgpg.skip=true
mvn -o test -Dgpg.skip=true
mvn -o verify -Dgpg.skip=true
```

相关模块及依赖定向 test 共 59 项；全仓 test 共 277 项，失败/错误/跳过均为 0。最终 verify 的实际输出摘录：

```text
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.712 s -- in dev.macula.boot.starter.cloud.gateway.utils.KongApiUtilsTest
[INFO] BUILD SUCCESS
[INFO] Total time:  01:19 min
```

Cloud 根目录：

```sh
mvn -o test -Plocal -Dgpg.skip=true
mvn -o verify -Plocal -Dgpg.skip=true
```

全仓 test 共 387 项，失败/错误为 0，跳过 1 项。最终 verify 输出摘录：

```text
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.638 s -- in dev.macula.cloud.gateway.TinyIdGatewayIT
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.034 s -- in dev.macula.cloud.system.form.ApplicationFormTest
[INFO] Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.086 s -- in dev.macula.cloud.system.service.impl.SysTenantServiceImplTest
[INFO] BUILD SUCCESS
[INFO] Total time:  44.467 s
```

Admin 目录：

```sh
npm run test:unit -- --run
npm run build
```

```text
Test Files  10 passed (10)
     Tests  60 passed (60)
✓ built in 5.22s
```

前端构建时间以 /tmp/macula-tenant-test-admin-build.log 原始输出为准；保留大分包等既有构建提示，不代表运行时性能验证。

另已运行 Boot/Cloud 本次 Java 文件的定向 mvn -o -N checkstyle:check（传入 checkstyle.includes），新增适配的 TinyIdGatewayIT 也单独执行相同检查，均为 BUILD SUCCESS。两仓 git diff --check 通过。Samples application.yml 使用已有 js-yaml 解析通过，保留 application.name，无手工 tenant-id。

## 首轮失败与修正

Cloud verify 首轮 TinyIdGatewayIT 发生 2 项失败：

```text
TinyIdGatewayIT.validatesSignatureThenForwardsVerifiableJwt
Expecting actual not to be null
TinyIdGatewayIT.propagatesDownstreamFailure
Expecting code to raise a throwable.
```

原因是该既有测试手工 new KongApiGlobalFilter，未注入 applicationName，也未提供新增租户映射。按当前默认拒绝规则，请求在进入 JWT 或下游前返回 403。仅修改测试夹具：注入 macula-cloud-gateway，提供归属租户 1 与认证租户 12 的独立数据，并加强 JWT 认证租户、其他网关归属及未知网关拒绝断言。没有削弱生产校验。

随后以 deploy profile 本地 install 最新 Boot Gateway 及依赖（跳过测试、跳过 GPG），供 Cloud 验证引用。重新执行全量 Cloud verify 通过，包含全部单位和默认启用的集成测试。无远端上传。

## Proof 与规范对应

| 证明项 | 实际证据与边界 |
| --- | --- |
| 忽略大小写、最长前缀及未命中 -1 | KongApiUtilsTest 8 项全部通过，包含非法/空映射及默认拒绝 |
| 普通及清理入口归属检查 | KongApiGlobalFilterTest、RmOpaqueTokenEndpointFilterTest 全部通过；未知网关、归属不匹配不进入下游/清理缓存 |
| 可信身份与认证租户 JWT | AddJwtGlobalFilterTest、TinyIdGatewayIT 通过；后者使用真实 RSA 编解码 JWT，Redis 和下游仍为测试隔离对象 |
| 保留 buildKey | 原键格式与最终认证租户键测试通过，不新增 HMAC 专属命名空间 |
| 应用字段保存、缺省、清空及权限 | SysApplicationServiceImplTest 7 项、ApplicationFormTest 4 项通过；JSON 使用项目实际 Jackson 3；Mapper/MapStruct 编译通过 |
| 租户缓存归属生命周期 | SysTenantServiceImplTest 8 项通过，覆盖初始化、创建/改编码/删除提交后刷新及回滚不发布；为回调级测试，不是实际数据库提交链路 |
| 应用缓存及异常上下文恢复 | SysApplicationServiceImplTest 通过，应用刷新不再写租户映射 |
| 认证租户下拉与长整型保持 | application-auth-tenant.test.js 在全量 Vitest 中通过，覆盖加载选项、打开编辑器、回显及清空提交 |
| 增量 SQL 与 samples 配置 | 静态检查 V12 仅新增可空 auth_tenant_id，不改 V1、不改写旧 tenant_id；samples YAML 保留原 application.name |

## 跳过与未验证项

Boot 3 项：OrderServiceIT、KafkaProducerIT 由项目 @Disabled 禁用；TinyIdClientIT 需要显式开启 TINYID_E2E 及对应远程服务。本轮未启用。

Cloud 10 项：SysUserUsernameUniqueMigrationTest 1 项；SecurityPolicyMigrationIT 1 项；SettingsPersistenceIT 6 项；IdentityProviderMigrationIT、ExternalCredentialLoginMigrationIT 各 1 项。均由仓库已有的显式环境变量条件控制，需要指定全新隔离数据库，本轮未启用。

实际运行包含 Boot 默认 Redis 集成测试及 Cloud TinyID Testcontainers 的临时 MySQL 测试，不把这些结果当作新 V12 的数据库升级证据。未执行现有业务数据库迁移，未做真实多网关端到端或浏览器人工操作验证。

这些环境专项仍未验证，不宣称生产或全环境验证完成。它们由仓库既有条件排除在默认验证套件之外；本轮批准规范的必需验证未要求开启这些专项。默认全量 verify、计划内证明项、前端测试与静态检查均通过，后续可以进行本地代码审查，不代表接受外部环境风险或授权发布。

## 配置与 Evals

未修改 CLAUDE.md、AGENTS.md、技能、hook、REVIEW.md 或 reviewer 定义；新增的 .sdlc 文档是本变更过程记录，不改变审查策略。本次无适用的配置 eval。

## 原始证据

- /tmp/macula-tenant-test-boot-gateway.log
- /tmp/macula-tenant-test-boot-all.log
- /tmp/macula-tenant-test-cloud-all.log
- /tmp/macula-tenant-verify-boot-all.log
- /tmp/macula-tenant-verify-cloud-all.log（最终成功重跑）
- /tmp/macula-tenant-test-admin-all.log
- /tmp/macula-tenant-test-admin-build.log
- /tmp/macula-tenant-test-boot-style.log
- /tmp/macula-tenant-test-cloud-style.log
- /tmp/macula-tenant-test-cloud-gateway-style.log

无提交、推送或部署。

## Default-scope verification gate
Verification is green.

这里的绿指批准计划内的证明项及仓库默认启用的全量测试。上列 13 项条件跳过仍保留，不计为通过、不改写运行条件、不放宽规范，也不授权数据库升级、远端 PR、提交、推送或部署。Rain 已回复 YES 确认本轮报告。

## 审查修复后的最新验证
- 修复旧快照覆盖：使用现有 Redisson 共享锁保护读取最新列表到发布缓存的全过程；新增并发重现和异常解锁用例。
- 修复普通管理员编辑已有跨租户认证应用：编辑页省略未变更的认证租户字段，后端权限规则保持不变。
- 重新执行全量 Cloud mvn -o verify -Plocal -Dgpg.skip=true：405 项，395 通过、10 条件跳过、0 失败/错误，BUILD SUCCESS。
- 重新执行全量 Admin Vitest：10 个文件、63 项通过；npm run build 通过。
- 定向后端单测 17 项通过，修复文件 Checkstyle 与 diff --check 通过。Boot 源码未在本轮修改，上述 Boot 全量 verify 结果继续适用。
- 仍未重新解释或启用 13 项原有外部环境条件测试。以下为当前源码最新证据，前文首次执行的摘录仅用于记录验证历史。

```text
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.814 s -- in dev.macula.cloud.gateway.TinyIdGatewayIT
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.194 s -- in dev.macula.cloud.system.service.impl.SysTenantServiceImplTest
[INFO] BUILD SUCCESS
```

最新原始日志：/tmp/macula-tenant-review-fix-verify-cloud.log、/tmp/macula-tenant-review-fix-tests.log、/tmp/macula-tenant-review-fix-ui-all.log、/tmp/macula-tenant-review-fix-ui-build.log、/tmp/macula-tenant-review-fix-style.log。

Verification is green（计划内默认启用验证范围，跳过项仍按上文如实保留）。

## 中断恢复后的证据一致性
- 恢复后核对审查快照中的 29 个来源文件，均与上述已验证第 2 轮版本一致，没有因中断产生源码漂移，未无理由重复执行测试。
- 第 2 轮独立复审和 fresh verify 均确认两个修复闭环，无新增 Important/Minor。此为静态审查证据，不能代替上列真实测试输出，也不是人工批准。
