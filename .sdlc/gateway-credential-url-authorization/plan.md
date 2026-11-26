# Plan: Gateway 凭据与 URL 授权调整
Status: accepted

## Files that change

- Gateway 主代码：`security/ResourceServerAuthorizationManager.java`、`security/ResourceServerConfiguration.java`、`filter/SignCheckGlobalFilter.java`。
- Gateway 测试：`security/ResourceServerAuthorizationManagerTest.java`、`security/ApiKeySecurityChainTest.java`、新增 `filter/SignCheckGlobalFilterTest.java`（沿用现有签名检查替身和响应断言）；`src/test/resources/application.yml`。
- 文档和部署样例：Gateway README、WebSocket Starter README、腾讯与阿里巴巴 Gateway 示例的 `application.yml` 和阿里巴巴示例 README、Gateway Archetype 的 `application.yml`。

上述 Gateway 文件均位于 `macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway`，Java 包根为 `dev/macula/boot/starter/cloud/gateway`。

## Order of work

1. 基于远端 main 建立独立候选分支，移入三个现有用户修改，保留原工作区。核对已合并 PR #61 的加密提交。
2. 将依赖现有 URL 角色映射的 Bearer 检查和 API Key/HMAC 分流继续放在当前授权管理器，整理现有改动中的匹配复用、Javadoc 与导入。保持原路径生成、预检早返回及凭据分流优先级。沿用 ROOT 和否定角色规则。读取失败或无法读取到非空 URL 权限快照时，API Key/HMAC 拒绝。现有 RedisTemplate 读取延迟到订阅，并按既有 boundedElastic 模式运行，避免阻塞 WebFlux 事件循环。计划只改 Boot，不变更 Cloud 的缓存发布。
3. 用既有 `API_CRYPTO_ERROR` 固定响应处理签名密钥解密失败，并断言异常详情不会发给客户端。保留正常签名处理。
4. 对齐三个旧构造调用，补齐 URL 规则与凭据分流回归。现有 API Key 链路测试拆分明确开放与有角色规则的路径，保留原 header/query、多 HTTP 方法、凭据验证及 Bearer 测试。移除示例配置和 README 的旧配置入口。说明现有 Bearer 路由的配置迁移，示例列明受保护 POST 及 WebSocket 握手使用的网关路径和角色规则。
5. 运行目标模块与依赖测试，检查 Java 样式及 Git diff。独立审查后按已明确授权推送新分支，创建 PR 并遵循 main 的保护条件。

## Risks

- 旧配置移除后，原只认证的接口需要角色映射。生成项目仍可配置现有 URL 规则，README 说明迁移。
- URL 权限取自 Redis。Cloud 刷新期间先删除当前映射再重建。Boot 将缺失或空映射视为尚无可核验快照，API Key 和 HMAC 在这段时间或空规则部署上拒绝；配置读取成功且至少存在一条 URL 规则后，未命中规则的请求才继续验证应用凭据。空角色条目仍是已命中规则。
- CORS、CSRF 和预检代码保持原有实现。授权要求适用于进入现有 URL 授权管理器的路由。沿用 `ignore-urls` 白名单，文档说明角色保护路径不要配置到该白名单。

## Proof

- 覆盖 API Key、HMAC 对匹配 URL 规则的拒绝和未匹配时进入验证链。覆盖缺失快照、Redis 读取异常、空角色条目、Redis 延迟读取、Bearer 正反角色、配置移除后普通用户无规则拒绝及 ROOT 行为。
- 回归签名密钥解密失败的固定结果和不进入下游，保留已有签名校验、预检和鉴权配置测试。
- Maven 目标模块及依赖测试、变更 Java Checkstyle、演示和模板旧配置引用检查、Git diff 检查；所有可用 PR 门禁通过后再合并。

## 实施记录

- 签名过滤器在固定错误响应之外移除包含解密密钥的签名原文日志。签名输入及过滤顺序保持现状。
