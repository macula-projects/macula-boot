# 统一网关 API Key/HMAC 的 GlobalFilter 认证与 JWT 转换

API Key 原先在 WebFilter 中认证并删除 Authorization，HMAC 则在 Gateway 阶段验签，导致授权分流和 JWT 转换依赖不同的身份传递方式。本次让两者分别在 GlobalFilter 中验证凭据、租户及 URL 许可，再把可信 principal 交给 AddJwtGlobalFilter 统一转换 JWT。API Key 跳过 OAuth introspection；普通 OAuth 保留认证和 URL 角色校验。

租户解析与准入由 TenantUtils 共用，业务租户优先 authTenantId、缺省回退归属 tenantId。API Key 与 HMAC 缓存通过类型段隔离；同时修复重复执行下游、空 OAuth Authentication 及凭据来源冲突处理。更新 AGENTS.md 的注释与常量使用规则。独立审查发现并修复 token 清理入口既有 DEBUG 日志泄漏凭据的问题，增加日志捕获回归测试。

验证：此前全仓 `mvn test` 309 项通过；最新隔离副本 `mvn verify` 通过，Surefire 310 项、Failsafe 50 条目中执行通过 47 项、3 项按既有外部服务条件跳过。578 个 Java/POM 校验一致，只有临时副本 5 个测试资源替换 Redis 端口；临时 Redis 8.10.2 已关闭，CI Redis 7 环境仍以 CI 结果为准。变更 Java 文件 Checkstyle 通过。未验证真实 IAM、Kafka/RocketMQ 或 TinyID 外部 E2E。

兼容变化：ApiKeyAuthenticationFilter 更名为 ApiKeyGlobalFilter，Bean 名为 apiKeyGlobalFilter；租户不明或跨归属的 API Key 会被拒绝，API Key 新增 permitUrls 校验；JWT 租户段使用 tn，旧缓存自然过期。checkSign 改为 `(exchange, secretKey)` 纯验签，旧 Redis 参数重载移除；完整应用认证使用 KongApiGlobalFilter.validate。token 清理过滤器构造器改为注入该验证器。详情见网关 README。

设计与证据：[intent.md](intent.md)、[spec.md](spec.md)、[plan.md](plan.md)、[test-report.md](test-report.md)、[review-findings.md](review-findings.md)。

审查：依据 REVIEW.md Standard 执行 Bugs、Security、Compliance，两轮审查。第二轮 posted 0、已报告抑制 1、verifier-rejected 0、Nit 0。第一轮发现的既有 Important 凭据日志泄漏已修复并由独立 verifier 确认关闭，未解决发现 0。

本文件是本地 PR 文案草稿，尚未创建或发布远端 PR。代码审查结果记录于 review-findings.md；人工批准前不合并或部署。
