# Plan: 5.x 安全修复当前实施范围
Status: accepted.

## 授权与范围纠正
Rain 已接受非 SQL 修复，并明确要求恢复原 MyDataPermissionHandler 后重新评估。此前参数上下文、参数化插件、词法方案及相关 SQL 回归不再实施，Git 历史保留其过程。当前计划只覆盖保留的修复和已授权草稿 PR。

## Files that change
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
1. 恢复任务起始 Handler、原生数据权限插件并删除配套新框架；保留 ROOT 租户切换限制，清理文档与测试中的孤立引用。
2. 保留已接受的 JWT 配置/独立 Bean 条件、Secret 校验、opaque 到期、10 MiB 原流程限额、敏感日志清理、id:/u: 分类。
3. JWT 说明集中在 Gateway Starter README，文档反映当前范围。集成测试显式提供测试 Secret，不依赖生产固定默认值。
4. clean 构建、相关回归与 Checkstyle；对当前撤回 delta 独立复核。只提交标明 SQL 待重评估和测试限制的草稿 PR，不合并、不发布。

## Risks
原 SQL 注入和 OR 分组风险尚在，恢复不代表风险消除；find_in_set 方言约束保持。默认公开签名材料及用户明确保留策略仍在。旧未分类 JWT 缓存不回读，但滚动升级中的旧实例需统一更新。

## Proof
Handler 与起始基线逐字节一致；生产/测试无参数框架引用，clean 后无残留类。ROOT 与配置回归、Gateway 缓存/Bean/请求体测试验证保留行为。完整历史命令、失败、基线对照与当前验证分别记录于 verification.md；不能把已撤销 SQL 测试称为当前通过。未改保护配置，无适用 eval。

## Actual deviation
因独立审查暴露 SQL 参数遍历遗漏，曾试验词法方案但未提交；Rain 随后要求恢复原实现，已完整撤销，当前不再推进替代 SQL 架构。
