## 概述

该模块为安全模块，以资源服务器的角色和网关交互。

1. 资源服务器的安全依赖，支持对JWT的验证，不依赖任何认证服务器
2. 内部REST服务依赖该模块，通过JWT来获取用户信息；
3. 网关或者直接对外的REST服务通过OpaqueToken验证登陆信息
4. 该模块只是验证用户，没有处理权限信息。Token验证后的角色信息是有的。通常情况下，网关使用OpaqueToken，然后获取用户角色，验证是否通过权限校验，生成JWT给后续的微服务识别身份

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-security</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

```yaml
spring:
	security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://127.0.0.1:9000/oauth2/jwks 			# JWT KEY的URL，这里是网关提供
macula:
  security:
    ignore-urls:																							# 忽略权限的列表
      - /api/token
      - /api/token/**
      - /swagger-ui/index.html
      - /v3/api-docs/swagger-config
```

## 核心功能

### SecurityUtils

```java
/**
 * {@code SecurityUtils} 安全助手
 *
 * @author rain
 * @since 2022/7/25 15:16
 */
public class SecurityUtils {

    public static String getCurrentUser() {
        if (SecurityContextHolder.getContext() != null && SecurityContextHolder.getContext()
            .getAuthentication() != null && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        }
        return null;
    }

    /**
     * 获取用户昵称/姓名
     *
     * @return nickname
     */
    public static String getNickname() {
        return Convert.toStr(getTokenAttributes().get(SecurityConstants.JWT_NICKNAME_KEY));
    }

    /**
     * 获取用户角色
     *
     * @return 角色Code集合
     */
    public static Set<String> getRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && CollectionUtil.isNotEmpty(authentication.getAuthorities())) {
            return authentication.getAuthorities().stream()
                .map(item -> StrUtil.removePrefix(item.getAuthority(), "ROLE_")).collect(Collectors.toSet());
        }
        return Collections.emptySet();
    }

    /**
     * 获取部门ID
     *
     * @return deptId
     */
    public static Long getDeptId() {
        return Convert.toLong(getTokenAttributes().get(SecurityConstants.JWT_DEPTID_KEY));
    }

    /**
     * 获取数据权限
     *
     * @return DataScope
     */
    public static Integer getDataScope() {
        return Convert.toInt(getTokenAttributes().get(SecurityConstants.JWT_DATASCOPE_KEY));
    }

    /**
     * 获取当前租户ID
     *
     * @return 租户ID
     */
    public static Long getTenantId() {
        return Convert.toLong(getTokenAttributes().get(GlobalConstants.TENANT_ID_NAME));
    }

    public static String getTokenId() {
        if (SecurityContextHolder.getContext() != null && SecurityContextHolder.getContext()
            .getAuthentication() instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken token =
                (JwtAuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
            if (token != null && token.getToken() != null) {
                return token.getToken().getId();
            }
        }
        return null;
    }

    /**
     * 判断用户是否为超级管理员
     *
     * @return true/false
     */
    public static boolean isRoot() {
        return getRoles().contains(SecurityConstants.ROOT_ROLE_CODE);
    }

    public static Map<String, Object> getTokenAttributes() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            if (authentication instanceof AbstractOAuth2TokenAuthenticationToken) {
                AbstractOAuth2TokenAuthenticationToken<?> tokenAuthenticationToken =
                    (AbstractOAuth2TokenAuthenticationToken<?>)authentication;
                return tokenAuthenticationToken.getTokenAttributes();
            }
        }
        return Collections.emptyMap();
    }
}
```

### 鉴权支持

- URL鉴权是在gateway实现的，本模块不支持URL鉴权
- 方法级别的注解鉴权：默认已经解锁 @PreAuthorize 和 @PostAuthorize 两个注解。

### 统一 CheckOwner 与列表授权 PassKey

资源级鉴权使用一个 `@CheckOwner` 注解，校验依据由 SpEL 表达式确定，与 HTTP 动词或客户端是否携带凭证无关：

| 场景 | 用法 | 时机 |
| --- | --- | --- |
| 数据有明确的当前用户归属 | `@CheckOwner("returnObject.buyerUsername")` | 方法返回后检查拥有者，无需列表或 PassKey |
| 无法直接判断用户与资源关系，依赖前置授权列表框定范围 | `@CheckOwner("#p0")` / `@CheckOwner("#p0.orderId")` | 方法执行前验证资源 PassKey |

位置参数支持 `#p0`、`#a0`；命名参数依赖参数名可发现，并按实际声明 `@CheckOwner` 的接口或实现方法解析，实现方法可以使用不同参数名。只支持同步单对象，入参资源编号支持非空 `String` 或整数，不支持集合。混合引用返回值与入参、常量或无法明确分类的表达式都会拒绝。两种模式失败不会互相降级，ADMIN、ROOT 不自动绕过。注解只适用于通过 Spring Bean 代理调用的公开方法，同类自调用或手动创建对象不会被保护。

先由业务按当前用户的数据权限查询列表；列表 DTO 实现 Commons 的 `dev.macula.boot.security.PasskeyResource`：

```java
@Data
public class OrderListItem implements PasskeyResource {
    private String orderId;
    private String passkey;

    @Override
    public String getResourceId() {
        return orderId;
    }
}
```

接口辅助 `resourceId` 带 `JsonIgnore`，不会额外出现在 JSON 中；DTO 自己的 `orderId` 正常输出。无需签发注解。**所有返回此类列表元素的入口必须已经完成资源授权**，不能在未授权公共列表复用 DTO；单个对象实现接口不自动签发。列表与详情 DTO 可以不同，后置归属模式不要求实现该接口。

启用配置（没有默认签名密钥）：

```yaml
macula:
  security:
    passkey:
      enabled: true
      ttl: 5m
      secret-base64: ${PASSKEY_SECRET_BASE64}
```

| 配置 | 默认 | 含义 |
| --- | --- | --- |
| `macula.security.passkey.enabled` | `false` | 开启资源凭证；关闭时列表正常返回且不签发，默认服务及 Clock 不装配；入参模式拒绝，后置归属模式不受影响 |
| `macula.security.passkey.ttl` | `5m` | 正数且至少 1 秒的凭证有效期 |
| `macula.security.passkey.secret-base64` | 无 | Base64 编码、解码后至少 32 字节的独立服务端密钥 |

所有实例应使用相同签名配置；不要使用登录 JWT 的默认密钥，也不要提交密钥。默认服务使用 HMAC-SHA256，将版本、可信 `authentication.name`、全局唯一资源编号和过期时间纳入明确字段编码，不包含租户、类型或 action。应用可以提供自己的 `PasskeyService` Bean 替换签发与验证，或提供 `Clock` Bean 替换时间源；自定义实现必须保留拒绝与身份校验约束。

现有 Web Starter 的 `ControllerResponseAdvice` 为授权列表签发，支持 `Collection`、`PageVO`、成功 `Result` 包装及 `ResponseEntity` 正文，混合列表只处理接口实现元素。`enabled=false` 时跳过列表遍历与签发，即使有自定义签发器也不会调用；启用时有候选列表但无有效签发器则拒绝。只有 Security Starter 而未接入 Web Advice 时不会自动签发，前置保护仍要求有效凭证。

```java
@CheckOwner("#p0")
@PreAuthorize("@pms.hasPermission('order:cancel')")
@PostMapping("/{orderId}/cancel")
public void cancel(@PathVariable String orderId) {
    orderService.cancel(orderId);
}

@CheckOwner("#p0.orderId")
@PreAuthorize("@pms.hasPermission('order:update')")
@PostMapping("/update")
public void update(@RequestBody UpdateOrderRequest request) {
    orderService.update(request);
}
```

前端携带 `X-Resource-Passkey` Header（推荐）或 URL Query 的 `passkey`。两处同值可用，重复、冲突、非法编码或空值拒绝；不读取表单同名字段。缺失或无效凭证在执行前拒绝，不进入业务方法。接入现有异常 Advice 后，资源拒绝返回 HTTP 403 与 A0312，且不包含详情。

凭证在到期前可以重复使用，业务授权变更不会立即撤销已签发凭证。编号必须全局唯一且稳定。PMS 继续负责按钮和操作权限，业务事务继续检查订单状态等实时条件；PassKey 不能替代租户或其他业务边界，后置归属检查只能用于无副作用查询。

### 内部接口注解@Inner

默认情况下依赖了本模块的接口都要有token才能访问，对于一些内部接口，比如定时任务调用的接口，没有经过网关，没有token，这个时候你可以在你的Controller上的方法上加上@Inner注解。调用方需要设置请求header为`from=Y`

```java

@FeignClient(value = "macula-cloud-system", url = "${macula.cloud.endpoint}", contextId = "systemFeignClient",
    configuration = FeignClientConfiguration.class)
public interface SystemFeignClient {
    @GetMapping(value = "/system/api/v1/menus/routes", headers = SecurityConstants.HEADER_FROM_IN)
    List<RouteVO> listRoutes();
}

public class TestController {
    @Operation(summary = "路由列表")
    @GetMapping("/api/v1/menus/routes")
    @Inner
    public List<RouteVO> listRoutes() {
        return systemService.listRoutes();
    }
}
```

## 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-commons</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
    </dependency>

    <dependency>
        <groupId>com.nimbusds</groupId>
        <artifactId>oauth2-oidc-sdk</artifactId>
    </dependency>
</dependencies>
```

## 版本说明

- oauth2-oidc-sdk：https://github.com/hidglobal/oauth-2.0-sdk-with-openid-connect-extensions/blob/master/LICENSE.txt
