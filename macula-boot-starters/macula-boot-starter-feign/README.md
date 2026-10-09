## 概述

引入spring-cloud-openfeign并添加了HMAC拦截器、请求头传递等功能。

## 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-feign</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

HTTP 客户端配置使用 `spring.cloud.openfeign.*` 前缀，不再使用旧的 `feign.*`。

```yaml
spring:
  cloud:
    openfeign:
      httpclient:
        hc5:
          enabled: false
        okhttp:
          read-timeout: 60s
      okhttp:
        enabled: true
macula:
  feign:
    header-relay:
      enabled: false
      headers: []
```

| 属性 | 默认值 / 示例 | 说明 |
| --- | --- | --- |
| `macula.feign.header-relay.enabled` | 默认 `false` | 开启额外请求头透传，不控制内置 Authorization、请求 ID 和灰度头 |
| `macula.feign.header-relay.headers` | 默认空列表 | 额外透传的头名称，只配置可信且必要的头 |
| `spring.cloud.openfeign.okhttp.enabled` | 示例 `true` | 选择已引入的 OkHttp 客户端 |
| `spring.cloud.openfeign.httpclient.hc5.enabled` | 示例 `false` | 使用 OkHttp 时禁用 HC5 客户端选择 |
| `spring.cloud.openfeign.client.config.<客户端名>.connectTimeout` | 按需设置 | 单客户端建连超时，单位毫秒；`default` 表示全局配置 |
| `spring.cloud.openfeign.client.config.<客户端名>.readTimeout` | 按需设置 | 单客户端读取超时，单位毫秒 |

## 核心功能

### 请求头传递

Servlet 应用默认注册 `HeaderRelayInterceptor`，传递请求 ID、Authorization 及灰度上下文；已有 Authorization 不会被自动覆盖。没有请求 ID 时会生成新值。额外请求头仅在 `macula.feign.header-relay.enabled=true` 时按 `headers` 白名单传递。

异步或非 HTTP 调用没有原始 Servlet 请求时，不能依赖请求头自动透传。

### 自动生成请求签名

```java
/**
 * {@code KongApiInterceptor} KONG认证的API访问拦截签名
 *
 * @author rain
 * @since 2022/7/27 08:59
 */
@AllArgsConstructor
public class KongApiInterceptor implements RequestInterceptor {

    @NonNull
    private String username;
    @NonNull
    private String secret;
    private String appKey;

    public KongApiInterceptor(@NonNull String username, @NonNull String secret) {
        this.username = username;
        this.secret = secret;
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        String date = DateUtil.formatHttpDate(new Date());
        requestTemplate.header("Date", date);
        // 签名，计算方法为 hmac_sha_256(key, 日期 + 方法 + 路径)
        HMac hmacSha256 = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes());
        String authorization = null;
        String method = requestTemplate.method();
        if (Request.HttpMethod.POST.name().equals(method) || Request.HttpMethod.PUT.toString().equals(method)) {
            // 请求体摘要
            MessageDigest messageDigest = null;
            try {
                messageDigest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            byte[] hash = messageDigest.digest(requestTemplate.body() == null ? new byte[] {} : requestTemplate.body());
            String digest = "SHA-256=" + Base64.encode(hash);
            requestTemplate.header("Digest", digest);
            // 签名，计算方法为 hmac_sha_256(key, 日期 + 方法 + 路径 + 摘要)
            String signature = hmacSha256.digestBase64(
                "date: " + date + "\n" + method + " " + requestTemplate.url() + " HTTP/1.1\ndigest: " + digest, false);
            authorization =
                "hmac username=\"" + username + "\", algorithm=\"hmac-sha256\", headers=\"date request-line digest\", signature=\"" + signature + "\"";
        } else if (Request.HttpMethod.GET.name().equals(method) || Request.HttpMethod.DELETE.toString()
            .equals(method)) {
            // 签名，计算方法为 hmac_sha_256(key, 日期 + 方法 + 路径)
            String signature =
                hmacSha256.digestBase64("date: " + date + "\n" + method + " " + requestTemplate.url() + " HTTP/1.1",
                    false);
            authorization =
                "hmac username=\"" + username + "\", algorithm=\"hmac-sha256\", headers=\"date request-line\", signature=\"" + signature + "\"";
        }
        if (authorization != null) {
            requestTemplate.removeHeader(SecurityConstants.AUTHORIZATION_KEY);
            requestTemplate.header(SecurityConstants.AUTHORIZATION_KEY, authorization);
        }
        if (appKey != null) {
            requestTemplate.header("appkey", appKey);
        }
    }
}
```

该拦截器仅在需要的时候定义，当通过@FeignClient定义远程调用代理的时候，可以自动生成相应的访问参数或者签名，例如：

```java
public class GapiConfiguration {

    @Value("${gapi.username}")
    private String username;
    @Value("${gapi.secret}")
    private String secret;

    @Bean
    KongApiInterceptor kongApiInterceptor() {
        return new KongApiInterceptor(username, secret);
    }

}

@FeignClient(name = "gapi-service", url = "https://gapi-dev.infinitus.com.cn", configuration = GapiConfiguration.class)
public interface GapiService {

    @PostMapping("/ecp/po/trade/updateEvaluationStatus")
    PoBaseResult updateEvaluationStatus(@RequestBody PoBaseDto poBaseDto);

    @GetMapping("/ecp/po/query/getOrderDetail")
    PoBaseResult getOrderDetail2Result(@RequestParam("poNo") String poNo);

}
```

### Feign调用异常处理

通过解包远程调用的返回来生成BizException

```java
/**
 * {@code OpenFeignErrorDecoder} 解决Feign的异常包装，统一返回结果
 *
 * @author rain
 * @since 2022/7/23 12:35
 */
@Slf4j
public class OpenFeignErrorDecoder implements ErrorDecoder {

    /**
     * Feign异常解析
     *
     * @param methodKey 方法名
     * @param response  响应体
     * @return ApiException
     */
    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("feign client error,response is {}:", response);
        try {
            // 获取数据
            String body = Util.toString(response.body().asReader(Charset.defaultCharset()));

            try {
                Result<?> resultData = JSONUtil.toBean(body, Result.class);
                if (!resultData.isSuccess()) {
                    String errMsg = "Feign提供方异常：";
                    if (resultData.getData() != null && !"null".equals(resultData.getData().toString())) {
                        errMsg = resultData.getData().toString();
                    } else {
                        errMsg += resultData.getMsg();
                    }
                    return new BizException(errMsg);
                }
            } catch (Exception ex) {
                return new BizException(body);
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }

        return new BizException("Feign提供方异常");
    }
}
```

### @FeignClient说明

{{% alert title="提示" color="primary" %}}

@FeignClient是支持远程访问微服务体系内的服务以及第三方服务的。

{{% /alert %}}

```java
// 访问第三方服务，直接给URL
@FeignClient(url = "${macula.cloud.endpoint}", contextId = "systemFeignClient", configuration = FeignClientConfiguration.class)
public interface SystemFeignClient {

    @GetMapping("/system/api/v1/users/{username}/loginUserinfo")
    UserLoginVO getUserInfoWithoutRoles(@PathVariable String username,
        @RequestParam(value = GlobalConstants.TOKEN_ID_NAME, required = false) String tokenId);

    @GetMapping("/system/api/v1/menus/routes")
    List<RouteVO> listRoutes();
}

// 访问注册中心名叫macula-cloud-system的微服务
@FeignClient(value = "macula-cloud-system", contextId = "userFeignClient", fallbackFactory = AbstractUserFeignFallbackFactory.class)
public interface UserFeignClient {

    @GetMapping("/api/v1/users/{username}/loginUserinfo")
    UserLoginVO getLoginUserInfoWithoutRoles(@PathVariable String username);
}
```

#### 远程访问的熔断处理

```java
@FeignClient(value = "macula-cloud-system", contextId = "userFeignClient", fallbackFactory = AbstractUserFeignFallbackFactory.class)
public interface UserFeignClient {

    @GetMapping("/api/v1/users/{username}/loginUserinfo")
    UserLoginVO getLoginUserInfoWithoutRoles(@PathVariable String username);
}
```

@FeignClient的fallbackFactory属性可以配置当远程访问有异常的时候怎么处理。

```java
@Component
@Slf4j
public class UserFallbackFactory extends AbstractProviderFallbackFactory {

    @Override
    public UserFeignClient create(Throwable cause) {
        log.error("异常原因:{}", cause.getMessage(), cause);
        return new Provider1Service() {
            @Override
            public UserLoginVO getLoginUserInfoWithoutRoles(String username) {
                return xxxx;
            }
        };
    }
}
```

## 依赖引入

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-openfeign</artifactId>
    </dependency>

    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-commons</artifactId>
    </dependency>

    <dependency>
        <groupId>io.github.openfeign</groupId>
        <artifactId>feign-okhttp</artifactId>
    </dependency>

    <dependency>
        <groupId>jakarta.servlet</groupId>
        <artifactId>jakarta.servlet-api</artifactId>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

## 版权说明

- spring-cloud-openfeign：https://github.com/spring-cloud/spring-cloud-openfeign/blob/main/LICENSE.txt
- feign-okhttp：https://github.com/OpenFeign/feign/blob/master/LICENSE
