# Spec: 6.x 安全审查修复（SQL 待重新评估）
Status: accepted.

## 当前有效范围
Rain 要求保留原 Handler，并明确 username 仅为大小写字母、数字、下划线。当前仅在原 SELF 分支完整校验该白名单、非法值返回 1=0、合法值用单引号，以及为原 WHERE 和权限条件分别加括号。原生插件、部门/find_in_set、ALL/DEFAULT 语义保留。参数上下文、参数化插件、词法重写方案继续撤销，不恢复。

## Requirements
1. 只有已认证且非匿名的 ROOT 可以采用请求租户；其他身份沿用认证租户及默认回退。TenantFilter 在 finally 清理上下文。
2. 网关 JWT KeyStore 可配置，保留原资源/别名/密码约定；显式错误不回退。JWKSource、JwtEncoder 分别退让，自定义编码器可依赖框架 source。
3. Secret 验证模式不再提供固定默认 HMAC Secret；显式 Secret 的 UTF-8 至少 32 字节，其他验证来源及自定义 decoder 保持。
4. opaque 身份缓存读取及新内省结果检查 exp；TTL 使用剩余有效期，无 exp 最多 5 分钟；阻塞 Redis 调度离开事件线程。
5. 请求体默认 10 MiB，可配置；保留原分支、Object 缓存短路、defaultIfEmpty/doOnNext/then 和链构造/订阅语义，聚合超限标准 413。
6. 删除请求/响应明文与解密密钥日志，不泄漏密码配置。
7. JWT 派生缓存只追加 id:/u: 分类，保留原 API Key（如适用）和租户段；不回读旧未分类键，撤销使用同一 buildKey。应用间隔离按用户明确的各网关独立缓存部署。
8. JWT 默认兼容和环境变量说明集中在 Gateway Starter README，不重复散布。

## 用户确认的最小 SQL 修复
Rain 明确用户名只包含大小写字母、数字、下划线。SELF 完整匹配 `[A-Za-z0-9_]+`，空值或非法字符返回恒假权限条件，不删除字符后映射成另一用户；合法值使用标准单引号字面量。仅补充原 WHERE 与权限条件的括号，保留原 Handler、原生插件、部门及 find_in_set 查询、ALL/DEFAULT 策略。不恢复参数框架或跨数据库改造。

## 数据库与身份边界
find_in_set 保持原 MySQL 方言，不宣称全数据库兼容；列名、别名仍属于代码注解的可信配置。白名单安全结论依赖用户名字符约定，未来若允许其他字符，需要重新评估，不可仅放宽正则后继续拼接。

## Non-goals
不改变 DEFAULT/缺失 dataScope、@Inner、invoker；不轮换真实密钥、不操作业务数据库/Redis、不合并或部署。保留原 Handler 结构，只采用已确认的白名单与分组修复。

## Compatibility
保持 Java 17、现有自动配置入口和默认签名材料；手工无参入口继续可用。

## Verification strategy
验证 ROOT/租户清理、JWT 默认/配置/Bean 回退、Secret 模式、缓存过期/TTL、请求体原流程/413、敏感日志及 id:/u: 撤销一致性。恢复 Handler 后执行 clean 构建及相关回归，核对 Handler 仅有最小修复、无已删框架引用。旧 SQL 参数化测试结果不适用于当前实现。受保护配置未改，无适用 eval。

## PR gate
主线最小修复已重新验证并独立审查，通过后进入 PR 人工评审；不自批准合并。5.x 的旧全仓阻塞单独记录。
