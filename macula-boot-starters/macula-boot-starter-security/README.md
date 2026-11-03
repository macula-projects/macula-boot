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

### 返回值鉴权 @PostAuthorize

拥有者可从查询结果明确判断时，直接使用 Spring Security 的 `@PostAuthorize`。需要在查询前拦截未登录用户时，同时标注 `@PreAuthorize("isAuthenticated()")`。拥有者字段必须采用与 `authentication.name` 相同的身份标识；默认 JWT 的 name 来自 sub。

```java
@PreAuthorize("isAuthenticated()")
@PostAuthorize("returnObject != null and returnObject.buyerUsername != null and returnObject.buyerUsername == authentication.name")
public OrderView getOrder(Long orderId) {
    return orderRepository.findViewById(orderId);
}
```

表达式按方法实际返回值编写，例如包装对象可以访问 `returnObject.data?.buyerUsername`；后续 Advice 的包装不属于方法返回值。ADMIN/ROOT 是否可放行由业务表达式明确决定。返回后拒绝不能撤销写入、消息发送等副作用，不用于阻止写操作；列表数据权限仍应在查询条件中落实。标准 PostAuthorize 异常由现有 Spring Security/业务异常处理负责，不自动套用 Passkey 的 HTTP 403/A0312 映射。

### 列表授权范围的 Passkey

`dev.macula.boot.starter.security.annotation.Passkey` 标注在 Spring MVC Controller 参数上，参数绑定成功后、Controller 执行前检查资源凭证。`@Passkey` 使用参数本身作为 resourceId；`@Passkey("orderId")` 读取 DTO 的 JavaBean 属性，`@Passkey("order.id")` 读取嵌套属性。编号只支持非空 String 或整数，不需要参数名或 `#p0` 等 SpEL 表达式。属性必须可通过 getter 读取；属性缺失、中间值为 null、非法路径、class 导航、方法调用、Map、索引或集合编号均拒绝。

凭证必须绑定当前真实登录用户与资源编号；ADMIN/ROOT 无隐式豁免。装饰器委托 Spring 原有解析器完成 PathVariable、RequestParam、RequestBody 的绑定及适用的 Valid 校验，只读取请求体一次，再验证请求凭证。未标注参数保持原处理。此功能只处理 MVC 请求，不保护 Service 方法或内部直接调用；Controller 上的 PreAuthorize/PMS 和 PostAuthorize 仍须通过，方法权限校验通过 Spring 代理执行。

迁移说明：旧方法上的 `@CheckOwner("#p0")` 移到对应 Controller 参数并改为 `@Passkey`；旧 `@CheckOwner("#p0.orderId")` 改为参数上的 `@Passkey("orderId")`；旧返回值用法改为上面的 PreAuthorize/PostAuthorize。旧注解、CheckOwnerAdvisor 和 CheckOwnerAccessDeniedException 已删除，不保留兼容入口，也不再提供方法级 Advisor。自定义签发/验证继续使用 PasskeyService；MVC 装饰基础设施为 PasskeyArgumentResolverPostProcessor。资源拒绝异常为 PasskeyAccessDeniedException。主线保留已有 AnnotationTemplateExpressionDefaults Bean，5.x 沿用旧方法安全基础设施。

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

接口声明 `getResourceId()`、`getPasskey()`、`setPasskey(...)`，辅助 resourceId 带 JsonIgnore，不额外出现在 JSON 中；DTO 自己的 orderId 正常输出。无需签发注解。**所有返回此类列表元素的入口必须已经完成资源授权**，不能在未授权公共列表复用 DTO；单个对象实现接口不自动签发。列表与详情 DTO 可以不同，使用 PostAuthorize 的查询不要求实现该接口。

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
| `macula.security.passkey.enabled` | `false` | 开启资源凭证；关闭时列表正常返回且不签发，默认服务及 Clock 不装配；标注 Passkey 的请求参数拒绝，标准 PostAuthorize 独立工作 |
| `macula.security.passkey.ttl` | `5m` | 正数且至少 1 秒的凭证有效期 |
| `macula.security.passkey.secret-base64` | 无 | Base64 编码、解码后至少 32 字节的独立服务端密钥 |

所有实例使用相同签名配置；不要使用用户登录 JWT 的默认密钥，不要提交密钥。默认服务使用 HMAC-SHA256，将版本、可信 `authentication.name`、全局唯一资源编号和过期时间纳入明确字段编码；不包含租户、类型、action。应用可提供自己的 `PasskeyService` Bean 替换签发与验证，或提供 Clock Bean 替换时间源。服务契约同时继承公共签发 SPI，替换后签发与验证使用同一实例；自定义实现必须保留拒绝与身份校验约束。

现有 Web Starter 的 ControllerResponseAdvice 为授权列表签发，支持 Collection、PageVO、成功 Result 包装及 ResponseEntity 正文，混合列表只处理接口实现元素。关闭/替换该 Advice 后须自行接入签发；`macula.security.passkey.enabled=false`（默认）时跳过列表遍历与签发，正常返回，即使有自定义签发器也不会调用。启用时有候选列表但无有效签发器则拒绝，配置无效时默认服务启动失败。只有 Security Starter 而未接入 Web Advice 时不会自动签发，前置保护仍要求有效凭证。

```java
@PreAuthorize("@pms.hasPermission('order:cancel')")
@PostMapping("/{orderId}/cancel")
public void cancel(@PathVariable @Passkey String orderId) {
    orderService.cancel(orderId);
}

@PreAuthorize("@pms.hasPermission('order:update')")
@PostMapping("/update")
public void update(@RequestBody @Valid @Passkey("orderId") UpdateOrderRequest request) {
    orderService.update(request);
}
```

前端携带 `X-Resource-Passkey` Header（推荐）或 URL Query 的 `passkey`。两处同值可用，重复、冲突、非法编码或空值拒绝；不读取表单同名字段，也不使用请求或返回 DTO 内的 passkey 字段验证请求。参数绑定成功后，缺失/无效凭证拒绝且 Controller 不执行；绑定或校验错误继续按 Spring 和应用既有异常处理。接入现有异常 Advice 后，资源凭证拒绝返回 HTTP 403 与 A0312，且不包含详情。

凭证在到期前可重复使用；业务授权变更不会立即撤销已签发凭证。编号必须全局唯一、表示稳定。PMS 继续负责按钮/操作权限，业务事务继续检查订单状态等条件；单资源凭证不能用于授权请求内的其他资源，也不能替代租户或其他业务边界控制。返回后鉴权仍只能用于无副作用的查询。

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
