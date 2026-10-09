# Plan: 发号链路完全移除旧 token

Status: accepted，用户 yes 确认。用户明确指出 token 已不用；不再保留此前的旧 token 发号兼容方案。

## Files that change

- Cloud TinyID：IdContronller、TinyIdIssuingService/Impl、application.yml、相关 Controller/Service/Web/Security 测试和 README。
- Cloud Gateway：路由 application.yml、TinyIdGatewayIT、README；不恢复已撤销的 TinyIdHmacGlobalFilter。
- 两仓 TinyID 任务 spec/plan/verification；相关旧 token 接口说明。

## Order of work

1. 四个发号接口移除 token 参数；Service 删除 TinyIdTokenService 注入和 requireToken，以及仅为 token 区分的重载。只保留 bizType 和适用的 batchSize。
2. Server 删除三个旧发号接口的匿名白名单，所有发号接口均要求有效 JWT；网关路由精确覆盖四个发号接口，沿用共享认证授权逻辑。按用户最新指示，不新增或扩展 HMAC-only 限制，HMAC 与个人 Token 的区分留待统一处理。
3. 同步测试、示例和迁移说明，删除“兼容旧 token 发号”的描述。

## Risks

- 旧 token 调用不再受支持，调用方须使用统一网关认证；Starter 使用 AK/SK，其他身份沿用共享策略。不能只删 token 校验而保留匿名入口。
- 此次仅移除发号链路的 token 依赖，不自行删除数据库授权数据、管理页面或仍被管理功能使用的 TinyIdTokenService。管理端遗留 token 功能的清理需另行明确范围。
- 不改变发号算法、成功响应或统一异常链路，不推送或部署。

## Proof

- 四个接口无 TinyID 专属 token 可通过合法网关身份调用；拒绝匿名请求，保留 HMAC 错误签名和无应用 URL 许可的检查，不额外拒绝普通 Bearer。
- Server 四个接口匿名请求均被拒绝；Controller/Service 契约不再含 token，管理授权和批量上限回归。
- 正式测试需重新执行，不沿用先前旧 token 兼容测试结果。
