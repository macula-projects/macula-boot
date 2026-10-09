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
      only-auth-urls: /api/**, /websocket/**
```

| samples 配置 | 说明 |
| --- | --- |
| `server.port` / `server.http.port` | Alibaba 为 HTTPS `5443` / HTTP `5000`，分别由 `SERVER_PORT` / `SERVER_HTTP_PORT` 覆盖；HTTP 扩展端口需示例中的配套 Java 配置 |
| `server.ssl.*` | Alibaba 示例使用 classpath 证书；生产替换证书及口令 |
| `spring.cloud.gateway.server.webflux.routes` | HTTP / WebSocket 路由；Tencent 示例转发到 `macula-example-tencent-consumer`，端口为 HTTP `4000` |
| `spring.cloud.gateway.server.webflux.globalcors` | Alibaba 示例提供全局 CORS；生产应收窄来源，不照搬通配规则 |
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
| `apikey.enabled` | `true` | 创建 API Key 认证过滤器 |
| `rm-opaque-token.enabled` | `true` | 创建 Token 缓存移除端点过滤器 |
| `rm-opaque-token-endpoint` | `/gateway/rm/opaqueToken` | 缓存移除路径 |
| `force-hmac-rm-opaque-token-endpoint` | `true` | 移除 Token 缓存时强制 HMAC 校验 |
| `security.ignore-urls` | 空列表并合并内置白名单 | 放行认证的路径 |
| `security.only-auth-urls` | 空列表 | 只认证、不检查 URL 角色权限的路径 |
| `security.default-url-require-check` | `false` | 未配置 URL 权限时是否要求鉴权，生产需明确评估 |

灰度过滤器的创建另受 `macula.cloud.gray.enabled` 控制（默认 `true`），与 `macula.gateway.gray.enabled` 的运行开关不同。应用还需提供 `spring.application.name`，OAuth2 凭证和签名材料须由部署环境注入。

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

`spring.security.oauth2.resourceserver.jwt.issuer-uri` 用于下游 JWT 的签发者标识，默认 `http://127.0.0.1:9010`。默认签名密钥从 `jwk/jose.jks` 加载，目前没有对应的 YAML 密钥配置项；生产应用应提供自己的 `JWKSource<SecurityContext>` Bean 替换内置实现。

将oauth2的token转换为JWT传递给微服务，微服务通过JWT获取用户信息和角色信息。

1. 前端访问gateway接口，在HTTP请求头添加`Authorization: Bearer xxxxx`
2. gateway收到token后，调用iam服务器的introspect url返回用户信息和角色信息（同时会以token有效期来缓存用户信息）
3. 通过AddJwtFilter将用户信息和角色信息转为JWT Token放入请求头给后续的微服务的安全模块校验认证（JWT也会缓存）

### AK/SK认证

将hmac的签名等信息转为JWT传递给微服务，微服务通过JWT获取用户信息和角色信息。

1. 如果请求携带hmac信息，则校验签名，根据appId检查应用，如果应用有租户ID，则设置到租户上下文
2. 通过AddJwtFilter生成JWT，具体同上

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
