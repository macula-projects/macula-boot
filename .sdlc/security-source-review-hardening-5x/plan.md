# Plan: 5.x 安全修复当前实施范围
Status: accepted.

## 授权与范围纠正
Rain 已接受非 SQL 修复，并明确要求恢复原 MyDataPermissionHandler 后重新评估。此前参数上下文、参数化插件、词法方案及相关 SQL 回归不再实施，Git 历史保留其过程。Rain 随后确认 username 字符集，当前仅补 SELF 白名单与条件分组；此前框架方案仍全部撤销。

## Files that change
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/main/java/dev/macula/boot/starter/mp/handler/MyDataPermissionHandler.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/test/java/dev/macula/boot/starter/mp/handler/MyDataPermissionHandlerTest.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/README.md`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/config/GatewayAutoConfiguration.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/config/GatewayProperties.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/config/JwtConfiguration.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/filter/AddJwtGlobalFilter.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/filter/BodyGlobalFilter.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/filter/CryptoGlobalFilter.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/security/CachingReactiveOpaqueTokenIntrospector.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/main/java/dev/macula/boot/starter/cloud/gateway/security/ResourceServerConfiguration.java`
- `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway/src/test/java/dev/macula/boot/starter/cloud/gateway/SecurityHardeningRegressionTest.java`
- `macula-boot-starters/macula-boot-starter-jpa/src/test/java/dev/macula/boot/starter/jpa/test/BlogRepositoryTest.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/README.md`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/main/java/dev/macula/boot/starter/mp/config/MyBatisPlusAutoConfiguration.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/test/java/dev/macula/boot/starter/mp/test/SecurityHardeningRegressionTest.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/test/java/dev/macula/boot/starter/mp/test/UserMapperTest.java`
- `macula-boot-starters/macula-boot-starter-mybatis-plus/src/test/java/dev/macula/boot/starter/mp/test/UserSeqMapperTest.java`
- `macula-boot-starters/macula-boot-starter-security/README.md`
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/config/ResourceServerConfiguration.java`
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/config/SecurityProperties.java`
- `macula-boot-starters/macula-boot-starter-security/src/main/java/dev/macula/boot/starter/security/utils/SecurityUtils.java`
- `macula-boot-starters/macula-boot-starter-web/README.md`
- `macula-boot-starters/macula-boot-starter-web/src/main/java/dev/macula/boot/starter/web/filter/TenantFilter.java`

## Order of work
1. 保留原 Handler 与原生插件，仅在 SELF 完整校验 username、非法值无匹配、合法值单引号，并给原条件与权限条件分别加括号；保留 ROOT 租户限制。
2. 保留已接受的 JWT 配置/独立 Bean 条件、Secret 校验、opaque 到期、10 MiB 原流程限额、敏感日志清理、id:/u: 分类。
3. JWT 说明集中在 Gateway Starter README，文档反映当前范围。集成测试显式提供测试 Secret，不依赖生产固定默认值。
4. clean 构建、相关回归与 Checkstyle；对当前撤回 delta 独立复核。只提交标明 SQL 待重评估和测试限制的草稿 PR，不合并、不发布。

## Risks
最小修复依赖用户确认的 ASCII 用户名约定，未来放宽字符集必须重新评估；find_in_set 方言约束保持。默认公开签名材料及用户明确保留策略仍在。旧未分类 JWT 缓存不回读，但滚动升级中的旧实例需统一更新。

## Proof
Handler 仅有已接受的白名单/单引号/分组改动；新增回归覆盖合法/非法/空值、不得清洗身份、原 OR、ALL/DEFAULT 与部门行为。生产/测试无参数框架引用。ROOT 与配置回归、Gateway 缓存/Bean/请求体测试验证保留行为。完整历史命令、失败、基线对照与当前验证分别记录于 verification.md；不能把已撤销 SQL 测试称为当前通过。未改保护配置，无适用 eval。

## Actual deviation
因独立审查暴露 SQL 参数遍历遗漏，曾试验词法方案但未提交；Rain 随后要求恢复原实现，已完整撤销，当前不再推进替代 SQL 架构。

## Accepted minimal username correction
- Rain 确认 username 字符集为大小写字母、数字、下划线，收窄到原 Handler 内的白名单校验与 OR 分组；不恢复任何参数上下文、额外 JDBC 查询或新插件。
- 先加入合法/非法/空用户名、非法值不可变成其他用户、OR 条件有效约束及 ALL/DEFAULT/部门原语义回归；确认旧实现失败，再修改 SELF 分支及 AndExpression 两侧括号。
- 非法用户名返回 1=0，不清洗后继续查询；合法用户名使用单引号消除 ANSI_QUOTES 的字面量歧义。部门 SQL 保持原样。同步两分支的简短 README 和验证记录。

## Minimal correction result
原 Handler 仅新增完整用户名白名单及两侧括号，合法值单引号，未恢复已撤销参数框架。新五项回归先红后绿；主线目标 43 项与全仓 426 项检查通过，5.x 六项相关回归通过且保留 Java 8。独立 Round 3 与 fresh verifier 均无待解决新发现；5.x 原全仓限制保持。
