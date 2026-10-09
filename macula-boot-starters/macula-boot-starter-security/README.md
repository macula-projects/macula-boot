## 概述

该模块为安全模块，以资源服务器的角色和网关交互。

1. 资源服务器的安全依赖，支持对JWT的验证，不依赖任何认证服务器
2. 内部REST服务依赖该模块，通过JWT来获取用户信息；
3. 网关或者直接对外的REST服务通过OpaqueToken验证登陆信息
4. 网关负责 URL 角色权限校验并向下游传递 JWT；本模块解析身份和角色，并支持在业务方法上通过 `@PreAuthorize`、`@PostAuthorize` 进一步鉴权。

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
          jwk-set-uri: http://127.0.0.1:9000/oauth2/jwks
macula:
  security:
    ignore-urls:
      - /api/token
      - /api/token/**
```

| 属性 | 默认值 / 行为 | 说明 |
| --- | --- | --- |
| `macula.security.ignore-urls` | 空列表，随后合并内置白名单和 `@Inner` 路径 | 无需认证的 URL，避免配置过宽 |
| `spring.security.oauth2.resourceserver.jwt.jwk-set-uri` | 应用指定 | JWT 公钥集地址，需与网关签发的 JWT 对应 |
| `spring.security.oauth2.resourceserver.jwt.issuer-uri` | 未设置 | 通过签发者地址发现验证配置 |
| `spring.security.oauth2.resourceserver.jwt.public-key-location` | 未设置 | 本地 RSA 公钥资源路径 |
| `spring.security.oauth2.resourceserver.jwt.jws-algorithms` | Boot 默认 `RS256` | JWK / 公钥方式允许的签名算法 |
| `spring.security.oauth2.resourceserver.jwt.secret` | 内置兼容值 | 对称密钥配置；生产环境应显式注入，不使用公开默认值 |

当未配置 JWK、issuer 或公钥路径且无自定义 JwtDecoder 时，使用 `secret` 构造对称验签器；不要在不同验签模式间混配属性。

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

URL 与角色的权限映射由 Gateway 处理；本模块的资源服务器配置已通过 `@EnableMethodSecurity` 开启方法鉴权，无需重复声明。

| 注解 | 校验时机 | 典型用途 |
| --- | --- | --- |
| `@PreAuthorize` | 方法执行前，不通过则不执行方法 | 角色、权限及入参校验，尤其是写操作 |
| `@PostAuthorize` | 方法正常返回后，不通过则抛出访问拒绝异常 | 根据查询结果的归属决定是否允许返回 |

以下为业务 Service 示例，`OrderRepository`、`OrderView` 由应用实现；`OrderView` 提供 `getOwnerUsername()`，其值需与当前认证用户名使用同一标识体系。

```java
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    // 调用前检查角色，只有管理员可以取消订单
    @PreAuthorize("hasRole('ADMIN')")
    public void cancel(Long orderId) {
        orderRepository.cancel(orderId);
    }

    // #p0 为第一个参数，不依赖编译时保留参数名
    @PreAuthorize("hasRole('ADMIN') or (isAuthenticated() and #p0 == authentication.name)")
    public long countByOwner(String username) {
        return orderRepository.countByOwnerUsername(username);
    }

    // 先要求登录，再根据实际查询结果检查归属
    @PreAuthorize("isAuthenticated()")
    @PostAuthorize("hasRole('ADMIN') or (returnObject != null and returnObject.ownerUsername == authentication.name)")
    public OrderView getById(Long orderId) {
        return orderRepository.findViewById(orderId);
    }
}
```

常用表达式及本模块的 JWT 权限映射：

| 表达式 | 含义 |
| --- | --- |
| `hasRole('ADMIN')` | 默认检查 `ROLE_ADMIN`，不要写成 `hasRole('ROLE_ADMIN')` |
| `hasAnyRole('ADMIN', 'ROOT')` | 任一角色满足即可；ROOT 不会自动绕过其他表达式 |
| `hasAuthority('ROLE_ADMIN')` | 精确匹配权限字符串，等价于默认前缀下的 ADMIN 角色检查 |
| `hasAuthority('SCOPE_orders.read')` | 精确匹配 JWT scope/scp 转换后的权限 |
| `authentication.name` | 当前认证用户名；本模块默认 JWT 转换器使用 `sub` |
| `returnObject` | 方法实际返回值，仅用于返回后校验；若返回包装对象，需访问其内部数据 |

默认转换器将 JWT `authorities` 中的 `ADMIN` 转为 `ROLE_ADMIN`，将 scope/scp 中的 `orders.read` 转为 `SCOPE_orders.read`。`hasAuthority('sys:user:del')` 不会自动查询平台按钮权限；需要平台权限服务时可配合 [System Starter](../macula-boot-starter-system/README.md) 使用 `@PreAuthorize("@pms.hasPermission('sys:user:del')")`，前提是已注册并配置 `pms` Bean。

`pms.hasPermission(...)` 的参数对应平台中配置的**按钮权限标识**，需与按钮配置保持一致。例如 `sys:user:del` 表示“删除用户”按钮权限，不是角色名或接口 URL。前端可用同一标识控制按钮显示，后端通过该注解校验操作权限；仅隐藏按钮不能替代后端鉴权。

使用限制：

- 通过 Spring Bean 代理调用才生效；同类内 `this.xxx()` 自调用、手动 `new` 对象不能依赖这些注解。示例使用可代理的公开方法。
- `@PostAuthorize` 执行时方法已经运行，不应用于防止写入、发消息等副作用，也不能假定拒绝返回会撤销已经发生的操作；写操作应在执行前检查权限。
- `@PostAuthorize` 不会逐条过滤集合。列表查询应在查询条件中限制用户、租户和数据范围，不能照搬单对象的归属表达式。
- 入参相等只证明请求的用户名相符，查询仍应按该用户过滤；多租户场景还需校验租户边界。角色名、scope 和业务字段均应替换为应用真实定义。

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
