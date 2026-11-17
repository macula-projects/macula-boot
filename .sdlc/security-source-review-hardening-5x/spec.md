# Spec: 5.x 安全审查修复（SQL 待重新评估）
Status: accepted.

## 当前有效范围
Rain 最新要求恢复原 MyDataPermissionHandler 后重新评估。当前 Handler 与任务起始基线完全一致，原生 DataPermissionInterceptor 接入恢复；参数上下文、参数化插件、词法改写及 SQL 专用测试全部撤销。历史 SQL 方案不再是本 spec 的要求，历史证据保留在 verification.md/review-findings.md 和 Git 历史中。

## Requirements
1. 只有已认证且非匿名的 ROOT 可以采用请求租户；其他身份沿用认证租户及默认回退。TenantFilter 在 finally 清理上下文。
2. 网关 JWT KeyStore 可配置，保留原资源/别名/密码约定；显式错误不回退。JWKSource、JwtEncoder 分别退让，自定义编码器可依赖框架 source。
3. Secret 验证模式不再提供固定默认 HMAC Secret；显式 Secret 的 UTF-8 至少 32 字节，其他验证来源及自定义 decoder 保持。
4. opaque 身份缓存读取及新内省结果检查 exp；TTL 使用剩余有效期，无 exp 最多 5 分钟；阻塞 Redis 调度离开事件线程。
5. 请求体默认 10 MiB，可配置；保留原分支、Object 缓存短路、defaultIfEmpty/doOnNext/then 和链构造/订阅语义，聚合超限标准 413。
6. 删除请求/响应明文与解密密钥日志，不泄漏密码配置。
7. JWT 派生缓存只追加 id:/u: 分类，保留原 API Key（如适用）和租户段；不回读旧未分类键，撤销使用同一 buildKey。应用间隔离按用户明确的各网关独立缓存部署。
8. JWT 默认兼容和环境变量说明集中在 Gateway Starter README，不重复散布。

## Deferred SQL review
原 SELF 用户名拼接可改变 SQL 条件，原 WHERE 含 OR 时缺少分组，尚未修复。find_in_set 是原有 MySQL 方言约束。下一步只基于原 Handler 评估注入与分组的最小修改，先确认范围，再写新的 SQL 实现；不把跨库通用化或参数基础设施默认混入。本次不声明 SQL 安全通过。

## Non-goals
不改变 DEFAULT/缺失 dataScope、@Inner、invoker；不轮换真实密钥、不操作业务数据库/Redis、不合并或部署。数据权限 SQL 重写已明确撤回。

## Compatibility
保持 Java 8 API/major 52、Boot 2.7、MyBatis-Plus 3.5.7、JSqlParser 4.9 与 javax。5.x 签名资源仍由应用提供，不新增私钥文件；不搬入 6.x API Key/IAM 协议。

## Verification strategy
验证 ROOT/租户清理、JWT 默认/配置/Bean 回退、Secret 模式、缓存过期/TTL、请求体原流程/413、敏感日志及 id:/u: 撤销一致性。恢复 Handler 后执行 clean 构建及相关回归，核对 Handler 与基线一致、无已删框架引用。旧 SQL 参数化测试结果不适用于当前实现。受保护配置未改，无适用 eval。

## PR gate
Rain 已选择“先提交标明验证限制的 5.x 草稿 PR”；旧日期/Redisson/外部服务测试阻塞尚在，不声明全仓为绿。
