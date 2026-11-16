# Plan revision: 复用网关 token 清理入口与资源服务器 API Key

Status: accepted; 用户明确回复 YES 接受本修订，开始实施

用户于本轮明确纠正原实现：`RmOpaqueTokenEndpointFilter` 已负责网关 token 缓存清理；应在 IAM 接入资源服务器上配置回调地址并增加 API Key，缓存至 Redis，由该过滤器验证后允许回调。本修订取代原 spec/plan 中新建 JWS 回调入口、网关账户版本缓存及派生 JWT 改造的部分。其余风险、MFA、账户管理及 IAM 本地撤销保持已批准要求。

## Files that change

Boot 模块根：`macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway`。

- 修改 `filter/RmOpaqueTokenEndpointFilter.java`、对应 `filter/RmOpaqueTokenEndpointFilterTest.java`：新增专用 API Key 准入分支，复用原清理实现；仅认证专用API Key，不保留HMAC兼容。
- `macula-boot-commons/src/main/java/dev/macula/boot/constants/CacheConstants.java`：定义资源服务器回调专用缓存前缀及字段，避免混用业务 API Key 权限。
- 撤销本功能对 `filter/AddJwtGlobalFilter.java`、`security/ResourceServerConfiguration.java`、`AddJwtGlobalFilterTest.java` 和自动配置 imports 的新增改动，保留任务开始前的原有内容。
- 移除本功能新增的 `config/IamRevocationAutoConfiguration.java`、`config/IamRevocationProperties.java`，以及 `security/revocation/` 下的 7 个新类型和对应新增测试。它们不承担本次修订后的协议职责；只处理本轮功能创建的文件，不清理其他改动。
- 更新模块 `README.md`，同步 SDLC 计划及实际验证记录。

Cloud：沿用原计划的文件范围，具体本次修改如下。

- System `form/IamResourceRevocationConfigForm.java`、`pojo/entity/SysIamResourceRevocationConfig.java`、`service/impl/IamResourceRevocationConfigServiceImpl.java`、配置 Controller/Service、System API `dto/IamResourceRevocationConfigDTO.java`：每个 RESOURCE_SERVER 的回调地址、专用随机 API Key、启用状态及配置版本；密钥生成与受控展示，不用 OAuth client_secret 充当回调凭据。
- System 新增 `service/cache/IamResourceCallbackCredentialCacheService.java` 及同名测试：数据库提交后发布 Redis 凭据，支持启动恢复/重建、禁用删除失效；同步失败显式报告，不能宣称可用。
- System `src/main/resources/db/migration/V30__iam_login_security.sql` 与相关实体/DTO：补充本功能尚未上线的新表字段及逐 token 投递记录；更新迁移和配置测试。
- IAM `security/impl/DatabaseIamSecurityStateRepository.java`、`protocol/oauth2/persistence/MaculaOAuth2AuthorizationService.java`、`revocation/impl/HttpIamRevocationDispatcher.java`：在失效前记录全部应清理的 access token，按资源服务器逐 token 调用原端点，保存重试进度。System 的管理员撤销入口 `service/impl/IamSecurityManagementServiceImpl.java` 使用相同撤销任务契约。
- 移除 IAM 本功能新增 `revocation/impl/JwsIamRevocationSigner.java` 及仅为它服务的测试，修改 `IamRevocationDispatcherTest.java`。
- Admin `src/views/system/oauth2Client/components/ResourceServerSettings.vue`、对应 API/语言资源/测试：在资源服务器接入页维护回调地址、API Key 与启用状态；默认示例为 `/gateway/rm/opaqueToken`。
- Cloud Gateway `src/main/resources/application.yml`：撤销本功能新增 JWS/版本缓存接入配置。
- 更新上述模块 README、IAM 配置文档及相关数据库/并发/管理端测试；原功能其余文件保持不变。

## Order of work

1. 经工程师接受后，将本修订合入两仓 spec/plan；清楚标识旧验证结果属于旧协议，不能作为新协议证据。
2. 回退仅由本功能引入的 Boot 独立协议代码，保留已存在的 token 缓存清理逻辑；清理入口仅API Key。
3. 在 IAM 资源服务器配置中增加专用 API Key；System 维护数据库与 Redis 发布、重建和失效。Key 使用独立命名空间，缓存绑定 resource_client_id、配置状态及指定回调路径。
4. `RmOpaqueTokenEndpointFilter` 仅接受 Header 中的专用 API Key，不通过 URL 传递；校验凭据、启用状态、与本网关的已配置内省 client_id 的绑定及端点，Redis 缺失/异常均拒绝。HMAC、无凭据和错误API Key一律拒绝；构造器直接注入sysRedisTemplate，不设置可选setter。
5. IAM 回调使用 `POST <资源服务器配置地址>`、`Authorization: Bearer <专用 API Key>`、JSON `{"token":"<access token>"}`；复用现有 Result 成功回执，不再发送 JWS 或要求版本回执。
6. 全部踢出覆盖该身份跨客户端 token，对全部登记资源服务器建立任务；未配置项显式未完成。逐 token 删除幂等；只有目标全部清理成功才能标记该资源服务器完成。保留持久化重试、超时、受限响应体和失败可见性。
7. 执行定向测试及必要集成测试，更新新协议实际结果。

## Risks

- 这是已批准跨服务安全契约的实质替换，工程师接受修订之前不修改源代码。不得以旧 JWS 测试结果宣布本修订完成。
- 本方案复用原网关缓存及派生 JWT 行为；不再承诺原新协议的版本水位、回填竞态保护或 0–300 秒收敛保证。缓存删除与并发内省可能短暂不一致，按用户已接受的尽力同步语义记录，不能宣称严格即时一致。
- 单 token 接口需要可恢复的 token 投递材料；不能只保存摘要后再尝试倒推出原值，也不能在撤销完成后调用已经禁止读取的授权查询。快照、用户版本和投递任务需协调，覆盖签发/刷新并发及同名不同目录身份隔离。
- 明文 token 仅用于已有清理协议的受控 HTTPS POST 与必要重试，禁止进入 URL、日志、普通列表或异常；任务只保留必要期限。API Key 与 OAuth 密钥隔离，不自动轮换现有凭据。
- 接收网关必须可读发布凭据的 Redis；多个实例需共享同一 token 缓存，独立 Redis 的资源服务器必须分别配置可用的发布/接入方式，不能假设本地缓存自动跨服务可见。
- 修改仅限本功能，保留任务前的改动；未上线新增迁移可修订，业务库和部署不在本次授权范围。

## Proof

- Boot：无效/缺失/重复 Header、Query 凭据、跨资源服务器 Key、停用与 Redis 异常均不能删除缓存；正确 Key 清理 opaque token 与派生 JWT；重复删除成功；明确验证HMAC和无凭据请求不能清理。
- System：资源服务器用途与 ROOT 权限、密钥不回显到普通读取、配置并发版本、提交后缓存发布/重建/禁用失效及失败报告。
- IAM：同一身份跨 client 多 token、所有资源服务器、逐 token 部分失败与重试、无配置状态、受限 HTTP 传输及原 Result 回执验证；签发/刷新与全部撤销并发、重复操作。
- Admin：回调 URL 与 API Key 管理、权限与密钥展示边界、加载失败不覆盖配置。
- 仅报告实际运行结果，真实外部资源服务器联调与部署另记。

## 用户追加约束：仅API Key且直接注入System Redis

用户明确要求无需兼容HMAC，仅认证API Key；直接注入System Redis，不增加可选setter/包装。此指示直接修订已接受计划：删除RmOpaqueTokenEndpointFilter的HMAC分支和forceHmacRmOpaqueTokenEndpoint属性；GatewayAutoConfiguration工厂直接传入sysRedisTemplate，删除setCallbackCredentials及认证中间标记；对应单测/Redis IT改为仅API Key，并覆盖HMAC/无凭据均拒绝。新增修改文件为GatewayProperties.java、GatewayAutoConfiguration.java、ApiKeyAutoConfigurationTest.java（验证直接绑定System Redis），均在原计划模块内。原业务HMAC过滤器职责不变。更新README与实际验证记录，不重复请求已给出的方向确认。
