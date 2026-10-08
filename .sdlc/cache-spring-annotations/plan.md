# Plan: Cache Starter 统一 Spring 7 推荐的 JSpecify 注解
Status: accepted

用户于 2026-10-08 要求统一 Spring 注解，随后指出 Spring 7 弃用警告，并明确同意将 TwoLevelCacheManager 和 TwoLevelCache 统一为 JSpecify。

## Files that change
- `macula-boot-parent/pom.xml`、`macula-boot-commons/pom.xml`：用户补充 JSpecify 1.0.1 版本管理及 Commons 直接依赖，随本次提交保留。
- `macula-boot-starters/macula-boot-starter-cache/src/main/java/dev/macula/boot/starter/cache/TwoLevelCacheManager.java`：使用 JSpecify NonNull，移除 NonNegative 并以参数文档保留约束说明。
- `macula-boot-starters/macula-boot-starter-cache/src/main/java/dev/macula/boot/starter/cache/TwoLevelCache.java`：将 Spring NonNull/Nullable 迁移到 JSpecify，泛型返回值注解放在返回类型前。
- `macula-boot-starters/macula-boot-starter-cache/pom.xml`：移除不再使用的 checker-qual。

## Order of work
1. 替换注解，不改变缓存计算、方法签名或运行时校验。
2. 删除直接依赖。
3. 执行目标模块及依赖单元测试、改动 Java Checkstyle、依赖树和差异检查。

## Risks
JSpecify 使用 TYPE_USE 注解，需调整泛型方法的返回值注解位置。没有等价 NonNegative 注解，使用文档说明而不增加运行时校验。JSpecify 1.0.1 已经由 Caffeine 在 compile 范围传递引入。

## Proof
复用现有缓存单元测试验证行为不变，编译证明注解位置合法，依赖树确认 checker-qual 是否仍被间接引入。

## Previous verification evidence
- 2026-10-08：`mvn -pl macula-boot-starters/macula-boot-starter-cache -am test` 通过，Cache 17 项、Redis 5 项，失败/错误/跳过均为 0。
- 针对 TwoLevelCacheManager.java 的 Checkstyle 通过，0 violations；`git diff --check` 通过。
- 目标 reactor 的 `dependency:tree -Dincludes=org.checkerframework:checker-qual` 无匹配，确认没有间接引入。
- 未执行外部 Redis 集成测试；缓存运行逻辑未改变。

## JSpecify verification evidence
- 2026-10-08：迁移后重新执行目标模块 `-am test`，Cache 17 项、Redis 5 项全部通过，失败/错误/跳过均为 0。
- 对 TwoLevelCache.java 和 TwoLevelCacheManager.java 执行 Checkstyle，0 violations；`git diff --check` 通过。
- 依赖树确认 JSpecify 1.0.1 已由 Caffeine 以 compile 范围传递引入，无需额外添加版本或依赖。
