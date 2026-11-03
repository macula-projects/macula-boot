# Plan: Passkey 单一前置资源凭证校验 (from spec.md 2026-11-03)
Status: accepted

## Files that change
下面路径相对各分支仓库根目录，主线和 5.x 具有相同文件范围。Security 模块前缀为 `macula-boot-starters/macula-boot-starter-security/`。

- `src/main/java/dev/macula/boot/starter/security/annotation/CheckOwner.java` 删除，新增同目录 `Passkey.java`。
- `src/main/java/dev/macula/boot/starter/security/authorization/CheckOwnerAdvisor.java` 删除，新增同目录 `PasskeyAdvisor.java`。
- `src/main/java/dev/macula/boot/starter/security/authorization/CheckOwnerAccessDeniedException.java` 删除，新增同目录 `PasskeyAccessDeniedException.java`。
- `src/main/java/dev/macula/boot/starter/security/config/SecurityAutoConfiguration.java`：迁移 Advisor 类型、条件及 Bean 名。
- `src/main/java/dev/macula/boot/starter/security/passkey/PasskeyRequestResolver.java`：更新资源拒绝异常类型。
- `src/main/java/dev/macula/boot/starter/security/passkey/HmacPasskeyService.java`：更新资源拒绝异常类型。
- `src/main/java/dev/macula/boot/starter/security/passkey/PasskeyService.java`：迁移默认列表签发方法中的认证拒绝异常类型及相关说明。
- `src/test/java/dev/macula/boot/starter/security/test/CheckOwnerTest.java` 删除，新增同目录 `PasskeyTest.java`，覆盖前置校验和标准 PostAuthorize 组合。
- `src/test/java/dev/macula/boot/starter/security/test/SecurityAutoConfigurationTest.java`：迁移类型、Bean 断言和配置组合调用。
- `src/test/java/dev/macula/boot/starter/security/test/PasskeyMvcTest.java`：迁移新注解，明确 Passkey 拒绝与标准 PostAuthorize 的响应区别。
- `src/test/java/dev/macula/boot/starter/security/test/PasskeyServiceTest.java`（仅主线修改）：补充与 5.x 相同的固定 golden token 断言；5.x 已有该用例，保持不变。
- `README.md`：替换统一 CheckOwner 用法，说明两种迁移方式和自定义 Advisor 覆盖点。

SDLC 证据（两条分支）：
- `.sdlc/passkey-only-authorization/intent.md`
- `.sdlc/passkey-only-authorization/spec.md`
- `.sdlc/passkey-only-authorization/plan.md`
- `.sdlc/passkey-only-authorization/review-findings.md`
- `.sdlc/passkey-only-authorization/verification.md`（Test 阶段生成）

Commons、Web 生产代码、POM、TSF 依赖以及历史 SDLC 文件不修改。自动配置仍使用原有 imports 入口，不新增配置类。

## Order of work
1. 获得计划批准后，为主线和 5.x 分别创建隔离的功能分支，保留现有工作区改动。同步本变更的已接受 intent/spec/plan；5.x 从当前已合并 PR #44 的基线开始。
2. 在主线添加 `@Passkey`、`PasskeyAdvisor`、`PasskeyAccessDeniedException`，移除旧三种类型。Advisor 只解析调用入参，验证真实登录、开关、请求、服务和凭证后才执行方法；删除返回值模式及 ResultRoot。保留实际注解声明方法的参数名解析、代理与桥接支持。解析或配置缺失产生受控访问拒绝，不暴露容器缺 Bean 异常。
3. 同步自动配置、请求解析和 HMAC 服务异常引用。保留原主线方法安全顺序与 AnnotationTemplateExpressionDefaults Bean，以免顺带破坏用户已有模板配置；Advisor Bean 改为 `passkeyAdvisor`，条件类型为 `PasskeyAdvisor`。
4. 重构主线测试：前置 Passkey 核心用例使用新 API；原拥有者用例改为标准 PreAuthorize/PostAuthorize；覆盖三个注解共存时前置拒绝计数为零、后置拒绝查询已执行。自动配置与 MVC 用例同步，标准 PostAuthorize 不强求公共资源异常的 HTTP 映射。
5. 更新 README，分别展示旧入参用法迁移到 Passkey、旧返回值用法迁移到 PreAuthorize/PostAuthorize；说明直接删除旧 API、自定义 Advisor 类型/Bean 名迁移和标准返回后检查限制。新增/迁移类型的类级文档遵循版权、`@author Rain` 和分支适用的 `@since`。
6. 将相同语义改写至 5.x，保持 Java 8、Jackson 2、javax.servlet 及 Spring Security 5.7。保留其已配置的方法安全优先级和全部 TSF 依赖，不引入主线新 API。
7. Build 阶段对两条分支运行目标 test-compile 和变更 Java Checkstyle，记录偏差；实现确认后进入 Test 阶段执行下述正式验证。阶段 5 分别对两条分支做独立代码评审并准备 PR；不合并、不部署。

## Risks
- 删除公开注解、Advisor、异常和 Bean 名导致下游源码/自定义覆盖不兼容；用户已确认不保留兼容入口，README 必须列明迁移方式，历史文档不做批量改写。
- 放弃前后置自动分类时不能错误接受 `returnObject`、未知变量、无来源字面量或空编号；表达式来源仍明确限制为入参。
- 两条分支的代理和方法安全顺序不同；所有前置保护必须在方法执行前生效，标准 PostAuthorize 必须仍能拒绝返回值。
- 标准 PostAuthorize 的错误类型不实现 ResourceAccessDenied，HTTP 映射遵循现有 Spring Security/业务处理；迁移测试不能错误保留原 CheckOwner 的 403/A0312 预期。
- 禁用 Passkey 后不能调用默认服务或专用 Clock；标注 Passkey 的方法必须拒绝，标准 PostAuthorize 独立工作。
- 保留已有模板 Bean 和外部签发协议，避免将本次 API 简化扩大成配置或密钥迁移。5.x 既有 TSF 全仓验证限制单独记录。
- 回滚使用每条分支本次修改的 revert；不自动回写下游应用的注解迁移。

## Proof
- `PasskeyTest`：String/整数编号、p/a 别名、命名与嵌套入参；非法/返回值/混合/未知/字面量/空值/错误类型表达式；无认证、匿名、ADMIN/ROOT、缺请求/凭证/服务、关闭开关与无效凭证；拒绝时业务计数不增加。
- 真实方法代理验证 JDK/CGLIB、接口与实现参数名不同、泛型桥接；PreAuthorize/PMS 与 Passkey 同时约束，三注解共存下合法调用成功、PMS 拒绝不执行、Passkey 拒绝不执行、PostAuthorize 拒绝时查询已执行但结果未返回。
- `SecurityAutoConfigurationTest`：新 Advisor 基础设施角色、可替换类型/Bean、默认与显式关闭、合法/非法启用、自定义服务和 Clock；主线保留模板 Bean 测试，5.x 不引入该 API。
- `PasskeyMvcTest`：现有列表签发到前置访问链路、Header/Query、PathVariable/RequestBody、写计数及 Passkey HTTP 403/A0312；返回后检查使用标准 PostAuthorize 并保留其当前响应约定。
- 主线 `PasskeyServiceTest` 增加与 5.x 相同的固定密钥、Clock、用户名、资源编号与 golden token 断言，两条分支都证明签发值相同且验证固定向量成功；保留请求提取、Commons 和 Web 用例，证明签发、配置、JSON 与响应行为未变。查找现行源码/测试中的 CheckOwner 残留，除明确的 README 迁移对照外应为零。
- 两条分支分别执行定向回归、Security/Web 及依赖 `clean verify`、变更 Java Checkstyle、`git diff --check`；5.x 使用真实 JDK 8。按仓库规则运行全仓测试，保留 TSF 基线限制，不更改无关依赖来追求全仓绿灯。
