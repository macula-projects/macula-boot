# Spec: Gateway 凭据与 URL 授权调整
Status: accepted

## Source intent

[已接受的需求](intent.md)。用户确认沿用工作区授权收紧行为、移除两个配置项，并同步验证与文档。签名密钥解密错误使用固定响应。

## Requirements

1. 普通 Bearer 请求执行现有 URL 角色授权。无匹配角色规则时普通用户失败，现有 ROOT 及反向角色行为保留。
2. API Key 和 HMAC 只能进入未匹配 URL 角色规则的接口，继续由各自的 GlobalFilter 验证凭据和已有准入条件。命中规则即拒绝，即使角色集合为空。Redis 映射缺失、为空或读取故障时拒绝应用凭据。Redis 查询在有界调度器运行。
3. 移除 `only-auth-urls` 和 `default-url-require-check` 的配置入口，更新 README、示例及生成模板。
4. 签名阶段密钥解密失败返回既有 `API_CRYPTO_ERROR`，响应不含底层异常、密钥或请求正文。

## Non-goals

保持 CORS、CSRF、预检、请求身份来源及原凭据核验次序。交付目标为 main。

## Design

按当前请求的应用名、HTTP 方法和原有 URL 规则匹配。角色授权仍在现有 ResourceServerAuthorizationManager，应用凭据命中 URL 角色规则时在原分流点终止，其它继续到已有凭据核验链。签名失败只使用现有固定结果码。

## Data and interfaces

继续读取现有 Redis URL 权限映射。无持久化数据变更。现有使用被移除配置的部署须为普通 Bearer 接口配置相应 URL 角色规则。

## Flagged concerns

- 原仅认证路由: 缺少 URL 角色配置时普通 Bearer 请求会失败。同步示例和文档说明配置迁移，non-blocking
- 浏览器访问: 预检处理沿用原有实现。需要验证跨域预检与实际请求分别遵循各自边界，non-blocking

## Verification strategy

测试两类应用凭据的匹配和不匹配、Bearer 角色允许和拒绝、空角色规则、现有预检、凭据核验链和固定错误。复跑 Gateway 及依赖、Crypto 验证和版本化样式检查。
