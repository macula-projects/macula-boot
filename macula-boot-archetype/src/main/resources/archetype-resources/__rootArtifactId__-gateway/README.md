# Gateway模块

基于Spring Cloud Gateway的微服务网关

## 请求体与访问限制

请求体缓存默认上限 10 MiB，签名/加密请求超限返回 HTTP 413；使用 `MACULA_GATEWAY_MAX_CACHED_BODY_SIZE` 可调整。此上限约束现有缓存分支，不能替代入口并发及上传策略。

资源服务器应继续使用已配置的 JWK 等验证来源；仅依赖旧默认 HMAC Secret 的下游应用需显式配置至少 32 字节的 Secret。请求租户切换仅允许 ROOT，普通身份继续采用认证租户。
