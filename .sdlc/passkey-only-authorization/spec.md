# Spec: Passkey 单一前置资源凭证校验 (from intent.md 2026-11-03)
Status: accepted

## Source intent
[intent.md](intent.md)：用户已确认主线与 5.x 同步改为 Passkey，仅执行前置凭证检查，直接删除 CheckOwner，返回后鉴权交给 PostAuthorize。

## Requirements
1. 新增方法级 `dev.macula.boot.starter.security.annotation.Passkey`，必填 `value` 表达式从方法入参取得一个 String 或整数资源编号，支持 `#p0`、`#a0`、命名参数和嵌套属性。
2. 移除 CheckOwner 注解、Advisor 及其命名的异常类型；新实现和自动配置使用 Passkey 命名，不提供旧 API 兼容入口。历史 SDLC 证据保留原文，现行文档和测试迁移到新 API。
3. 仅进行调用前校验；缺少真实认证身份、缺请求、缺凭证、非法表达式、返回值表达式、无入参来源、空编号、错误编号类型、关闭功能、缺少服务或凭证验证失败时拒绝，目标方法不得执行。ADMIN/ROOT 无隐式豁免。
4. 与 PreAuthorize/PMS 独立叠加；同一方法上的 Passkey、PreAuthorize 和 PostAuthorize 均须生效。保留 JDK/CGLIB 代理、接口注解和泛型桥接方法支持。
5. 返回值拥有者校验完全由标准 PostAuthorize 承担；不在 Passkey Advisor 中读取返回值或执行拥有者比较。文档给出独立 PostAuthorize 及与 Passkey 组合的示例。
6. 保持现有 Passkey 服务和列表签发协议：绑定用户、资源编号和有效期，Header/Query 提取规则、HTTP 403/A0312 的资源凭证拒绝、默认关闭语义、可替换服务/Clock、HMAC 与 golden token 均保持。
7. 主线与 5.x 具有一致外部 API 和安全语义，分别遵守现有依赖和 Java 版本；不调整 TSF/SnakeYAML 配置。

## Non-goals
不修改签发算法、TTL、密钥配置、列表容器范围、PMS/租户权限模型、网关认证链或数据库；不扩展标准 PostAuthorize 的异常映射，不修复无关全仓依赖问题，不合并或部署。

## Design
将统一 CheckOwner Advisor 收敛为 Passkey Advisor：解析实际承载注解的方法与入参，验证表达式只引用入参资源，提取请求凭证并验证后调用目标方法。删除返回值模式、ResultRoot 和前后置分类状态；保留对非法/未知来源表达式的拒绝。资源凭证拒绝异常改为 Passkey 命名并继续实现公共 ResourceAccessDenied 标记。

SecurityAutoConfiguration 注册可替换的 Passkey Advisor。保留现有方法安全顺序：主线位于 PreAuthorize 后；5.x 方法安全拦截器先于 Passkey，所有前置拒绝均阻止业务执行。其他 Starter 只在现行文档或组合测试引用旧类型时同步迁移，不改变 Commons/Web 的签发实现。

标准返回后示例使用 `@PreAuthorize("isAuthenticated()")` 和 `@PostAuthorize("returnObject != null and returnObject.ownerUsername != null and returnObject.ownerUsername == authentication.name")`。返回值条件由业务指定；不在示例中引入管理员自动放行。

## Data and interfaces
破坏性 Java API 迁移：`@CheckOwner("#p0")` 改为 `@Passkey("#p0")`；原 `@CheckOwner("returnObject.ownerUsername")` 改为标准 PostAuthorize 表达式，按需加 PreAuthorize。Advisor、Bean 与异常命名同步迁移并说明自定义覆盖点。配置键、Header、Query、JSON 和凭证格式不变，无数据库变更。

## Flagged concerns
- 删除 CheckOwner 是源代码不兼容变更：non-blocking，用户已明确选择直接移除，README 必须给出两种旧用法的迁移示例。
- PostAuthorize 不能阻止方法副作用，也不会自动继承原 CheckOwner 的前置认证和 HTTP 403/A0312 映射：non-blocking，文档明确只用于结果鉴权、需要前置认证时组合 PreAuthorize，并沿用标准 Spring Security 异常处理。
- 两条分支方法安全基础设施不同：non-blocking，分别验证 PMS 顺序、代理类型以及 Passkey 与 PostAuthorize 同时标注的真实调用行为。
- 5.x 的 TSF 依赖配置可能继续限制全仓测试：non-blocking，遵守用户已明确要求保留配置，报告范围内结果与基线限制。

## Verification strategy
重命名并重构注解/Advisor/自动配置测试，覆盖前置成功与所有拒绝路径；通过方法计数证明拒绝时不执行；覆盖接口参数名、JDK/CGLIB、桥接方法、ADMIN/ROOT 和 PMS 组合。新增标准 PostAuthorize 的允许/拒绝及与 Passkey 组合回归，返回后拒绝时查询已执行。保留服务、请求提取、Web 自动签发和 golden token 测试。

主线按现有工具链、5.x 按真实 JDK 8 执行定向测试和目标模块 clean verify，运行变更 Java Checkstyle 与 git diff --check；跨模块验证按仓库测试规则执行，现有 TSF 基线限制如实记录。无新增组织级品牌/合规技能；遵循 AGENTS.md、architecture.md、starter-development.md、testing.md 和 REVIEW.md。
