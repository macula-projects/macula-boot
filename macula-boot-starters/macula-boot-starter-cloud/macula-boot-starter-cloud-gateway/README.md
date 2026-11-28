## 概述

网关服务模块，给每个平台应用依赖用的。主要提供token认证、鉴权、接口加解密等功能。

## 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-cloud-alibaba</artifactId>
    <version>${macula.version}</version>
</dependency>

<dependency>
<groupId>dev.macula.boot</groupId>
<artifactId>macula-boot-starter-cloud-alibaba-scg</artifactId>
<version>${macula.version}</version>
</dependency>
```

## 使用配置

下面摘取当前 [Alibaba Gateway application.yml](../../../macula-boot-examples/macula-example-alibaba-gateway/src/main/resources/application.yml) 的 local 网关配置，需与 [Alibaba 配置](../macula-boot-starter-cloud-alibaba/README.md) 的公共及 profile 段合并，不是完整启动文件。Tencent 对应配置见 [Tencent Gateway application.yml](../../../macula-boot-examples/macula-example-tencent-gateway/src/main/resources/application.yml)。

```yaml
spring:
  config:
    activate:
      on-profile: local
  data:
    redis:
      host: 127.0.0.1
      port: ${REDIS_PORT:36379}
      password: ${REDIS_PASSWORD:redis}
  cloud:
    gateway:
      server:
        webflux:
          routes:
            - id: macula-example-alibaba-consumer
              uri: lb://macula-example-alibaba-consumer
              predicates:
                - Path=/consumer/**
              filters:
                - StripPrefix=1
            - id: macula-example-alibaba-consumer-ws
              uri: lb:ws://macula-example-alibaba-consumer
              predicates:
                - Path=/websocket/**
              filters:
                - StripPrefix=1
  security:
    oauth2:
      resourceserver:
        opaquetoken:
          client-id: e4da4a32-592b-46f0-ae1d-784310e88423
          client-secret: secret # 仅为 samples 演示值，部署时替换
          introspection-uri: ${OAUTH2_INTROSPECTION_URI:http://127.0.0.1:9010/oauth2/introspect}
  reactor:
    context-propagation: auto
macula:
  gateway:
    security:
      ignore-urls: /consumer/hello2/**,/consumer/api/v1/consumer/echo/**

```

| samples 配置 | 说明 |
| --- | --- |
| `server.port` / `server.http.port` | Alibaba 为 HTTPS `5443` / HTTP `5000`，分别由 `SERVER_PORT` / `SERVER_HTTP_PORT` 覆盖；HTTP 扩展端口需示例中的配套 Java 配置 |
| `server.ssl.*` | Alibaba 示例使用 classpath 证书；生产替换证书及口令 |
| `spring.cloud.gateway.server.webflux.routes` | HTTP / WebSocket 路由；Tencent 示例转发到 `macula-example-tencent-consumer`，端口为 HTTP `4000` |
| `spring.cloud.gateway.server.webflux.globalcors` | Alibaba 示例仅允许列出的本地来源；生产须替换为实际可信前端来源 |
| `spring.data.redis.*` | samples 使用标准单 Redis 配置；独立 System Redis 见下文扩展 |
| `spring.security.oauth2.resourceserver.opaquetoken.*` | Token introspection 地址及调用凭证 |
| `spring.reactor.context-propagation` | `auto`，跨 Reactor 线程传播上下文 |

示例普通环境关闭 OTLP 网络导出；`observability` profile 开启三类导出并指向 `otel-collector:4318`，适用于对应 Docker 网络。完整配置以源文件为准。

下列属性前缀为 `macula.gateway`：

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `sign-switch` / `crypto-switch` | `true` / `true` | 签名 / 加解密全局开关；对应过滤器还需 CryptoService Bean |
| `force-sign` / `force-crypto` | `true` / `false` | 是否强制校验已配置的受保护接口 |
| `protect-urls.sign` / `protect-urls.crypto` | 空列表 | 需签名 / 加密的路径 |
| `trace-id-response-header-enabled` | `true` | 有有效 Span 时输出 `x-traceId` |
| `gray.enabled` | `false` | 启用网关灰度路由处理 |
| `apikey.enabled` | `true` | 允许 API Key 接入；关闭后仍保留 GlobalFilter，拒绝携带 API Key 的 Gateway 请求 |
| `rm-opaque-token.enabled` | `true` | 创建 Token 缓存移除端点过滤器 |
| `rm-opaque-token-endpoint` | `/gateway/rm/opaqueToken` | 缓存移除路径 |
| `security.ignore-urls` | 空列表并合并内置白名单 | 放行认证的路径 |

灰度过滤器的创建另受 `macula.cloud.gray.enabled` 控制（默认 `true`），与 `macula.gateway.gray.enabled` 的运行开关不同。应用还需提供 `spring.application.name`，OAuth2 凭证和签名材料须由部署环境注入。

### URL 角色规则迁移

移除原来的 `macula.gateway.security.only-auth-urls` 和 `macula.gateway.security.default-url-require-check` 配置。曾经只校验 Bearer Token 的业务和 WebSocket 路径需要在发布到 Redis 的 URL 权限映射中配置角色。规则使用 `应用名:HTTP方法:网关接收路径`，例如 `sample-gateway:GET:/api/orders/**`。角色路径应排除在 `security.ignore-urls` 白名单之外。

API Key 和 HMAC 使用所属应用的接口授权。普通 Bearer 继续按现有角色、ROOT 和反向角色规则鉴权。

## 核心功能

### Trace ID 响应头

网关通过 Micrometer `Tracer` 读取当前 Span，并在有效上下文中返回兼容的 `x-traceId` 响应头。
无 Tracer、无当前 Span或关闭以下开关时不会产生空响应头：

```yaml
macula:
  gateway:
    trace-id-response-header-enabled: true
```

### Token认证

`spring.security.oauth2.resourceserver.jwt.issuer-uri` 用于下游 JWT 的签发者标识，默认 `http://127.0.0.1:9010`。签名密钥支持以下网关配置。未配置时保留原 `classpath:jwk/jose.jks`、`jose` 别名及原密码，旧部署签发密钥和下游验证关系不变；默认组件加载内置材料时输出 WARN。内置私钥公开，生产应配置独立签名材料，这次兼容调整不消除默认密钥风险。

| 属性 | 默认值 / 行为 |
| --- | --- |
| `macula.gateway.jwt.key-store` | `classpath:jwk/jose.jks`；支持 `classpath:` 或 `file:` |
| `macula.gateway.jwt.key-store-type` | `JKS`，可配置 `PKCS12` |
| `macula.gateway.jwt.key-store-password` | 保留原默认值，生产通过环境变量注入 |
| `macula.gateway.jwt.key-alias` | `jose` |
| `macula.gateway.jwt.key-password` | 未提供时使用 KeyStore 密码 |
| `macula.gateway.max-cached-body-size` | `10MB`，即 10,485,760 字节；必须大于零且不超过 2,147,483,647 字节 |

可使用 Spring 标准环境变量 `MACULA_GATEWAY_JWT_KEY_STORE`、`MACULA_GATEWAY_JWT_KEY_STORE_TYPE`、`MACULA_GATEWAY_JWT_KEY_STORE_PASSWORD`、`MACULA_GATEWAY_JWT_KEY_ALIAS`、`MACULA_GATEWAY_JWT_KEY_PASSWORD` 覆盖，也可在配置中心设置同名属性。显式资源、密码或别名错误会启动失败，不静默回退。

自定义 `JWKSource<SecurityContext>` 或 `JwtEncoder` Bean 分别按类型优先。仅替换编码器时，仍创建默认 JWKSource，兼容 `JwtEncoder customEncoder(JWKSource<SecurityContext> source)` 的原有装配方式；因此默认 source 的显式 KeyStore 配置也必须有效。使用独立密钥体系的编码器应同时提供对应的 JWKSource。密码不进入配置对象 toString 或异常消息。

签名/加密的 POST、PUT、PATCH 请求在本过滤器首次聚合时按实际字节数限制，包括未知 Content-Length 的分块流；聚合超限通过标准 HTTP 413 异常终止后续订阅。已有非 null 缓存对象按原逻辑直接传递，不转换类型、不重复读取或重新限额；保留原 defaultIfEmpty/doOnNext/then 链及链构造时机。普通未进入缓存分支的请求保持原行为。可用 `MACULA_GATEWAY_MAX_CACHED_BODY_SIZE` 调整，不能用 0 或负数关闭限制。过滤器不记录密钥和请求/响应明文。

Opaque Token 缓存命中仍校验 `exp`，过期缓存删除并重新内省；写入 TTL 使用从当前时间到 `exp` 的剩余时间，而非 `exp - iat`。非法 `exp` 拒绝认证；无 `exp` 时最多缓存 5 分钟。缓存键和撤销接口保持不变。

派生 JWT 的缓存键在原前缀后增加类型段：有 `JWT_ID` 使用 `id:`，否则使用 `u:`；API Key 与 HMAC 分别使用 `apikey:`、`hmac:` 前缀，并保留租户段，以隔离不同认证来源的派生身份。升级不回读旧派生键，旧值自然过期，撤销入口继续调用 `AddJwtGlobalFilter.buildKey`。应用间隔离沿用各网关独立缓存的部署方式。

将oauth2的token转换为JWT传递给微服务，微服务通过JWT获取用户信息和角色信息。

1. 前端访问gateway接口，在HTTP请求头添加`Authorization: Bearer xxxxx`
2. gateway收到token后，调用iam服务器的introspect url返回用户信息和角色信息（同时会以token有效期来缓存用户信息）
3. 通过AddJwtFilter将用户信息和角色信息转为JWT Token放入请求头给后续的微服务的安全模块校验认证（JWT也会缓存）

### IAM 资源服务器 API Key 回调

复用 `POST /gateway/rm/opaqueToken`（由 `macula.gateway.rm-opaque-token-endpoint` 配置），请求为 `Authorization: Bearer <专用 API Key>`、JSON `{"token":"<access token>"}`。IAM 在资源服务器接入设置中维护实际 HTTPS 回调地址，首次保存生成独立 Key，与内省 OAuth client_secret 分开。成功响应沿用 `Result.success()`。

`RmOpaqueTokenEndpointFilter` 从 `sysRedisTemplate` 读取 System 发布的凭据摘要和路径，按 `spring.security.oauth2.resourceserver.opaquetoken.client-id` 绑定本网关身份。只接受单一 Header 凭据，拒绝 Query、重复 Header、错误 Key、错误资源服务器、错误路径及缺失/不可读取的缓存；失败不能降级绕过认证。清理端点仅认证 API Key；无凭据及 HMAC 请求均拒绝，没有认证关闭开关。构造器直接注入 `sysRedisTemplate`，自定义装配也须直接传入System凭据缓存连接。

System 缓存键为 `CacheConstants.IAM_CALLBACK_CREDENTIAL_PREFIX + resource_client_id`，Hash 含 `apiKeyDigest`（SHA-256十六进制）和 `path`，仅启用的真实资源服务器配置发布，禁用即删除；60秒TTL，30秒从数据库重建。此凭据不赋予普通业务 API 权限。网关的System Redis必须可读该缓存，token缓存仍使用原网关Redis；集群实例必须共享token缓存，否则需分别配置回调。

通过认证后只执行 opaque token 与关联派生 JWT 缓存清理。IAM 对账户所有待撤销access token逐个回调所有资源服务器，部分失败保留重试；这是尽力同步，允许与并发内省存在短暂不一致，不承诺版本水位或固定60秒收敛，也不取消已开始的请求。内省缓存和派生 JWT 沿用原实现。

```bash
mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am test
# 必须使用专用临时Redis，不能指向业务Redis。
IAM_CALLBACK_REDIS_IT_PORT=<isolated-port> mvn -pl macula-boot-starters/macula-boot-starter-cloud/macula-boot-starter-cloud-gateway -am verify \
  -Dit.test=RmOpaqueTokenCallbackIT -Dfailsafe.failIfNoSpecifiedTests=false
```

### AK/SK认证

HMAC 验签由 `KongApiGlobalFilter` 负责，通过后构造 `OAuth2AuthenticatedPrincipal` 保存到请求属性，交给 `AddJwtGlobalFilter` 统一转换为下游 JWT。

1. 携带 HMAC 信息时校验签名、应用归属租户及 URL 许可；从可信验签结果取得业务租户，不写入 ThreadLocal 租户上下文。
2. `KongApiGlobalFilter` 构造 principal，保留 `invoker` 覆盖主体及 `tokenId` 对应 JWT `jti` 的既有规则。
3. `AddJwtGlobalFilter` 从已验证 principal 生成 JWT；OAuth、API Key 和 HMAC 共用请求头改写及 access_token Query 清理流程，各自缓存键规则保持。

API Key 与 HMAC 过滤器均在 `boundedElastic` 上执行验证阶段的阻塞 Redis 查询；验证成功后发布 principal，再进入下游过滤链。HMAC 保留原验签错误码、消息及原因，验证阶段的异常兜底不吞掉下游业务异常。

HMAC 分流使用 `KongApiUtils.resolveHmacToken(exchange)`，仅读取 Authorization Header；`KongApiUtils.verifiedPrincipal(exchange)` 只读取已验证 principal，缺失时拒绝，不代替验签。

### API Key 认证

API Key 与 HMAC 均在 Gateway `GlobalFilter` 阶段验证，通过后由 `AddJwtGlobalFilter` 转换为下游 JWT：

```text
Spring Security 输入检查与分流 → API Key 校验（order=190）/ HMAC 校验（order=200）
→ 统一 principal 转 JWT（order=1000）→ 下游服务
```

调用方可使用 `Authorization: Bearer sk-xxx`，或 `access_token=sk-xxx` Query 参数（支持 GET、POST、PUT、PATCH、DELETE 等方法）。API Key 不会发送到 OAuth introspection；普通 OAuth Bearer 保持原认证及 URL 角色检查。API Key 只允许一个凭据来源；重复 Authorization、重复 access_token、Header 与 Query 同时携带凭据以及非法格式会被拒绝。Query 可能被接入侧访问日志记录，优先使用 Header。

API Key 记录位于 System Redis 的 `macula:cloud:system:application:apikey:{key}` Hash，要求非空字符串 `applicationCode`、`applicationId` 和合法的 `tenantId`。网关通过共享的 `TenantUtils.resolveGatewayTenantId` 执行应用名/租户编码最长前缀匹配，解析 `spring.application.name` 对应的归属租户；只有该租户与 API Key 的 `tenantId` 一致才允许接入。应用名、映射或密钥归属缺失、归属不匹配以及 Redis 查询失败均拒绝请求；调用方租户 Header 不能改变归属。升级前须准备好 `macula:cloud:system:tenants` 和密钥归属缓存，历史跨租户或缺少租户的 API Key 将无法继续调用。

API Key 与 HMAC 使用所属应用在 System Redis 中的接口授权，并共用 `TenantUtils.checkTenantAndIp` 校验归属租户及来源 IP，解析认证业务租户。JWT 的 `tenantId` 优先使用 API Key 记录的 `authTenantId`，未配置时回退到归属 `tenantId`；认证租户不影响归属准入，非法租户值拒绝认证。验证成功的身份通过请求属性传递，不创建虚拟 OAuth token，也不依赖 SecurityContext。JWT 保留 `sub`、`applicationId`、`nickname`、`authType=apikey` 和 `tenantId`，共用 JWT 缓存键规则，仅增加 `apikey:` 类型段，默认键为 `macula:cloud:gateway:jwt:apikey:u:tn{tenantId}:{applicationCode}`，存在 `jti` 时优先使用它作为身份段。各认证来源的租户段统一为 `tn{tenantId}:`，旧 `a{tenantId}:` 缓存不再命中并等待自然过期。转发前覆盖 Authorization 为 JWT，并移除原始 access_token Query 参数。

`macula.gateway.apikey.enabled=false` 会拒绝 API Key Gateway 请求，包括 `ignore-urls` 中的路由。过滤器仍装配，避免关闭功能后原始凭据直接转发。此机制仅作用于 Gateway 路由；本地 WebFlux 端点不会获得 API Key 认证身份，需使用该端点自己的认证机制。

迁移注意：`ApiKeyAuthenticationFilter` 重命名为 `ApiKeyGlobalFilter`，Bean 定义移至 `ResourceServerConfiguration`，与 `KongApiGlobalFilter` 集中管理，Bean 名为 `apiKeyGlobalFilter`，接口由 `WebFilter` 改为 `GlobalFilter`，不再支持 `filter(exchange, WebFilterChain)`。可提供自定义 `ApiKeyGlobalFilter` Bean 覆盖默认实现；定制实现须验证凭据、遵守开关并按可信身份契约写入 `GatewayConstants.APIKEY_PRINCIPAL_ATTRIBUTE`，否则 JWT 转换会拒绝请求。HMAC 定制过滤器同样须构造已验证 principal 并写入 `GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE`；无需新增自动配置 imports 或依赖。

### URL安全

根据用户的角色和URL所需角色对比，控制URL权限。

1. macula-cloud-system模块在维护菜单、角色等信息后会定时将URL和角色关系缓存到redis

2. gateway根据请求URL匹配redis中的URL角色关系找出访问该URL所需角色

3. 根据当前用户的角色列表是否满足上一步的角色要求决定是否放行

{{% alert title="提示" color="primary" %}}

可以配置URL是否认证或者鉴权

{{% /alert %}}

### 接口的加解密和签名

网关要支持接口加解密和签名的话，首先要实现CryptoService，加解密所需方法。比如接入密钥服务系统。

```java
/**
 * {@code CryptoService} 接口加解密服务
 *
 * @author rain
 * @since 2023/3/22 19:36
 */
public interface CryptoService {

    /**
     * 获取用于加密前端生成的SM4Key的公钥
     *
     * @return 公钥
     */
    String getSm2PublicKey();

    /**
     * 解密前端传过来经过非对称加密的SM4 KEY
     *
     * @param key 加密过的sm4 key
     * @return SM4KEY明文
     */
    String decryptSm4Key(String key);

    /**
     * 加密数据
     *
     * @param plainText 明文
     * @param sm4Key    加密的密钥
     * @return base64密文
     */
    String encrypt(String plainText, String sm4Key);

    /**
     * 解密数据
     *
     * @param secretText base64密文
     * @param sm4Key     解密的密钥
     * @return 明文
     */
    String decrypt(String secretText, String sm4Key);

}
```

本地加解密实现示例如下：

```java
/**
 * {@code CryptoLocaleServiceImpl} 本地加解密服务
 *
 * @author rain
 * @since 2023/3/23 22:01
 */
@Component
@Slf4j
public class CryptoLocaleServiceImpl implements CryptoService, InitializingBean {
    private SM2 sm2;

    @Override
    public String getSm2PublicKey() {
        return HexUtil.encodeHexStr(sm2.getPublicKey().getEncoded());
    }

    @Override
    public String decryptSm4Key(String key) {
        return sm2.decryptStr(key, KeyType.PrivateKey);
    }

    @Override
    public String encrypt(String plainText, String sm4Key) {
        return SmUtil.sm4(sm4Key.getBytes(StandardCharsets.UTF_8)).encryptBase64(plainText);
    }

    @Override
    public String decrypt(String secretText, String sm4Key) {
        return SmUtil.sm4(sm4Key.getBytes(StandardCharsets.UTF_8)).decryptStr(secretText);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        KeyPair pair = SecureUtil.generateKeyPair("SM2");
        sm2 = SmUtil.sm2(pair.getPrivate(), pair.getPublic());
        if (log.isDebugEnabled()) {
            log.debug("sm2 public key: {}", HexUtil.encodeHexStr(sm2.getPublicKey().getEncoded()));
            log.debug("sm2 private key: {}", HexUtil.encodeHexStr(sm2.getPrivateKey().getEncoded()));
            log.debug("sm4 encrypted key: {}", sm2.encryptBase64("1234567890abcdef", KeyType.PublicKey));
        }
    }
}
```

#### 前端流程

- 密钥协商
    - 前端获取需要加密或者签名的接口：/gateway/protect/urls，返回{ crypto: [], sign: [] }，如果与当前请求匹配，则执行下述流程
    - 前端获取公钥：/gateway/protect/key
    - 前端根据URL规则判断是否要加密和签名
    - 前端随机产生一串密钥key并使用SM4公钥加密，将该密钥放入HTTP请求头`sm4-key`
- 请求加密
  - 前端对GET的Query参数param1=value1&param2=value2用上述随机串key进行SM4加密（注意param要排序）
  - 前端对POST的JSON Body用上述key进行SM4加密
    - 前端GET请求的加密参数附加在URL?data=xxx中，POST请求的加密参数也是以JSON格式放在data这个key中
  - 加密后以Base64编码（GET请求要encodeURI，Base64含有+=/等符号）
  - 请求头添加sym-alg，标识加密算法SM4
- 请求签名（加密后的，非空参数值才参与签名）
    - 生成当前时间戳，为UTC 1970年1月1日0时开始的毫秒数(Unix 时间戳)
    - 随机生成nonce随机串，注意要保证随机唯一性
    - GET请求签名sha256(path+param1=value1&param2=value2...+key+timestamp+nonce)
    - POST请求签名sha256(POST签名体 = path+param1=value1&param2=value2...+SHA-256=sha256(body))
      +key+timestamp+nonce)
    - SHA256是16进制字符串格式
    - timestamp、signature、nonce、algorithm（默认SHA-256）放入Header
    - 签名算法支持MD2、MD5、SHA-1、SHA-256
- 响应解密
  - 响应体的加密内容在JSON串的data这个key中，使用SM4解密

#### 后端流程

- 验证签名
    - 后端根据URL规则和macula.gateway.force-sign判断是否强制验证签名
    - 后端根据验签规则验证签名，不通过则返回错误
    - 验签成功后，网关用 Redis 原子登记 `macula:cloud:gateway:nonce:{nonce}`。有效期至少 300 秒，签名时间领先网关时覆盖剩余的签名有效窗口。同一 nonce 只允许登记一次。
- 请求解密
    - 后端根据macula.gateway.force-crypto判断是否强制加解密，如果请求URL在列表中但是没有携带sm4-key则返回错误
    - 后端解密请求数据，然后将加密的返回数据替换Result的data
- 响应加密
    - 根据需要对响应加密，放入Result的data字段中

### 配置System的Redis

最新 samples 使用 `spring.data.redis.*` 配置单个 Redis，没有额外声明 System Redis 连接。若业务需要访问独立的 macula-cloud-system Redis，可按以下方式扩展；`spring.data.redis.system.*` 是此示例手动绑定的自定义前缀，并非 Boot 自带的第二数据源配置。

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: ${REDIS_PORT:36379}
      password: ${REDIS_PASSWORD:redis}
      system:
        host: ${SYSTEM_REDIS_HOST:127.0.0.1}
        port: ${SYSTEM_REDIS_PORT:6379}
        password: ${SYSTEM_REDIS_PASSWORD}
```

此扩展示例新增的 `SYSTEM_REDIS_*` 变量需自行提供，最新 samples 中未定义这些变量。

```java
/**
 * {@code RedisConfiguration} Redis配置
 *
 * @author rain
 * @since 2023/4/21 11:50
 */
@Configuration
public class RedisConfiguration {
    @Bean
    @Primary
    @ConfigurationProperties(prefix = "spring.data.redis")
    public DataRedisProperties redisProperties() {
        return new DataRedisProperties();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.data.redis.system")
    public DataRedisProperties sysRedisProperties() {
        return new DataRedisProperties();
    }

    @Primary
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient(ApplicationContext ctx, DataRedisProperties redisProperties) throws Exception {
        Config config = RedissonConfigBuilder.create().build(ctx, redisProperties, new RedissonProperties());
        return Redisson.create(config);
    }

    @Bean(destroyMethod = "shutdown")
    public RedissonClient sysRedissonClient(ApplicationContext ctx, DataRedisProperties sysRedisProperties)
        throws Exception {
        Config config = RedissonConfigBuilder.create().build(ctx, sysRedisProperties, new RedissonProperties());
        return Redisson.create(config);
    }

    @Bean(name = "sysRedisTemplate")
    public RedisTemplate<String, Object> sysRedisTemplate(
        @Qualifier("sysRedissonClient") RedissonClient sysRedissonClient) {
        //数据泛型类型
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        //设置连接工厂（Jedis或Lettuce）
        template.setConnectionFactory(new RedissonConnectionFactory(sysRedissonClient));

        //设置key的序列化方式---String
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        //初始化RedisTemplate的参数设置
        template.afterPropertiesSet();
        return template;
    }
}
```

## 依赖引入

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-redis</artifactId>
    </dependency>

    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-commons</artifactId>
    </dependency>

    <dependency>
        <groupId>com.nimbusds</groupId>
        <artifactId>oauth2-oidc-sdk</artifactId>
    </dependency>

    <dependency>
        <groupId>com.github.ben-manes.caffeine</groupId>
        <artifactId>caffeine</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- oauth2-oidc-sdk：https://github.com/hidglobal/oauth-2.0-sdk-with-openid-connect-extensions/blob/master/LICENSE.txt
- caffeine：https://github.com/ben-manes/caffeine/blob/master/LICENSE
- spring-cloud-gateway：https://github.com/spring-cloud/spring-cloud-gateway/blob/main/LICENSE.txt

## AK/SK 应用租户

网关使用自己的 `spring.application.name`，从 System 缓存的“租户编码 → ID”映射中查找归属租户。匹配规则为 应用名以租户编码开头，不区分大小写；多个编码匹配时使用最长前缀。例如 `macula-samples-gateway` 匹配 `macula-samples`，对应租户 12。

不再需要配置 `macula.gateway.tenant-id`。应用名缺失、映射缺失、无匹配前缀或命中的租户 ID 非法时，解析结果为 `-1`，拒绝 AK/SK 接入；应用归属租户必须与解析出的网关租户一致。该规则同时适用于启用 HMAC 的 opaque token 清理入口，以及 API Key 的网关归属租户检查。

租户管理负责维护 `macula:cloud:system:tenants` Hash，字段为租户编码，值为租户 ID。System 启动时初始化，租户新增、修改和删除在事务提交成功后刷新，不依赖应用变更；映射未就绪或重建期间网关会拒绝未确认归属的请求。

应用缓存的 `tenantId` 是归属租户，`authTenantId` 是认证后的业务租户。JWT 的 `tenantId` 优先使用 `authTenantId`，未配置时回退到归属租户；非法认证租户值拒绝认证。调用方不能通过租户头改变本网关的归属。

先升级网关并准备好租户映射缓存，再启用不同于归属租户的认证租户。旧网关忽略 `authTenantId`；回滚旧网关前须恢复两者一致。

`TenantUtils` 统一负责网关归属租户解析，供 API Key 与 HMAC 使用。租户解析调用统一使用 `TenantUtils.resolveGatewayTenantId`。

`KongApiUtils.checkSign(exchange, secretKey)` 仅使用服务端传入的密钥校验请求时间、签名及请求内容，不查询 Redis、不检查租户或 URL 许可，也不发布身份。原接收 Redis 和路径/应用名的三参数、四参数 `checkSign` 重载已移除；自定义集成须迁移，不能把纯验签成功当成完整应用认证。

完整应用认证由 `KongApiGlobalFilter.validate(exchange)` 负责：读取应用缓存 → 归属及来源 IP 准入 → 纯验签 → 应用接口授权 → 构造 principal。返回成功后，GlobalFilter 发布 principal 并恢复请求体。该方法包含阻塞 Redis 查询，响应式调用方须使用 `boundedElastic`，提前缓存请求体并处理验证异常。验证结果不写入 `TenantContextHolder`。token 清理端点独立使用资源服务器 API Key，构造器第三个参数为 `sysRedisTemplate`，不再依赖此 HMAC 验证器。

### 应用与 API Key 的 URL/IP 准入

System Redis 按归属租户与应用 ID 发布 `macula:cloud:system:app_perms:{tenantId}:{applicationId}`。授权值为 `METHOD:/入口路径模式` 列表，支持 Ant 路径模式。API Key 与 HMAC 在认证后、发布 principal 前匹配改写前的网关入口路径。无匹配、缓存缺失或不可用时拒绝。Spring Security 只把待认证的应用凭据交给实际匹配的 Gateway 路由，其他受保护的 WebFlux 处理器拒绝这类凭据。普通 Bearer 继续使用角色 URL 规则。

新增缓存字段 `permitIps`，每行一个 IPv4、IPv6 或 CIDR：

```text
192.0.2.10
192.0.2.0/24
2001:db8::/32
```

缺失或空白表示不限；非空时任一规则匹配即通过 IP 检查，来源地址缺失、未命中或任一条规则非法均拒绝。URL 与 IP 两个条件都须满足，准入完成后才发布可信身份。每份配置最多 2000 字符。

来源为请求的直连对端地址，不自行读取 `Forwarded` 或 `X-Forwarded-For`；经过代理时可能匹配代理 IP。部署须避免由不可信转发头重写对端地址，真实调用者 IP 需先建立可信代理边界。IPv4-mapped IPv6 按等价 IPv4 处理，mapped CIDR 前缀必须不少于 96。


### CORS 与 CSRF 边界

Spring Security 在认证前复用 `spring.cloud.gateway.server.webflux.globalcors` 规则，使允许来源的浏览器也能读取 401 等认证失败响应；Gateway 路由层使用同一配置。`add-to-simple-url-handler-mapping: true` 可覆盖未匹配路由谓词的预检。未配置有效 CORS 规则时，不会增加默认开放来源，同源及非浏览器认证行为保持原样。

`/gateway/protect/key` 与 `/gateway/protect/urls` 早于安全链处理，端点自身执行跨域校验；拒绝和预检均提前结束，不生成配置正文。

生产环境使用明确的 `allowed-origins`，包含协议及必要端口。Alibaba 示例只列出 HTTP `localhost`/`127.0.0.1:5099` 与 HTTPS `localhost`/`127.0.0.1:5443`；额外的独立前端端口须显式加入。示例保留允许方法和请求头的通配规则以兼容演示，生产应按接口需要收窄，并保留实际 Authorization、Content-Type 及签名请求头。

纯 Bearer Header 认证不要求 `allow-credentials: true`，示例将其关闭。网关沿用显式 Token 认证及 CSRF 禁用；若代理的登录、刷新或业务接口依赖 Cookie/Session，须单独设计 CSRF 防护，不能仅依赖 CORS。Query Token 兼容行为保持不变，应避免在日志中记录凭据。
