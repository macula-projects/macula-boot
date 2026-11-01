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

### 数据拥有者校验 @CheckOwner

对无副作用的同步单对象查询，可以使用 `@CheckOwner` 统一检查返回值的归属。业务通过必填的 `value` 指定完整 SpEL 表达式；表达式结果必须非空，并与当前认证的 `authentication.name` 相等。默认 JWT 配置中的 name 来自 `sub`，与 `SecurityUtils.getCurrentUser()` 返回的标识一致。拥有者必须采用相同的身份标识及兼容类型，不能直接假定数字用户 ID 等于用户名。

```java
import dev.macula.boot.starter.security.annotation.CheckOwner;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;

    @CheckOwner("returnObject.buyerUsername")
    public OrderView getOrder(Long orderId) {
        return orderRepository.findViewById(orderId);
    }
}
```

其他业务可以指定不同属性，例如 `@CheckOwner("returnObject.createdBy")`。如果被标注的方法本身返回 `Result<OrderView>`，且订单在 `data` 中，则使用 `@CheckOwner("returnObject.data?.buyerUsername")`；`?.` 在 data 为空时返回 null，随后拒绝返回。如果 Result 是由 `ResponseBodyAdvice` 在后续包装的，表达式仍应按被标注方法的实际返回值填写，不加 `data`。

Starter 在现有 `SecurityAutoConfiguration` 中注册 `AnnotationTemplateExpressionDefaults`，无需应用额外声明配置类。应用可以提供同类型 Bean 覆盖默认值；该 Bean 控制 Spring Security 的全局注解模板解析，替换时必须保持所需模板行为。

安全边界：

- 未登录会在方法执行前拒绝；返回值为空、拥有者为空或不匹配会在方法返回后拒绝。ADMIN、ROOT 不自动绕过。
- 拥有者应从服务端真实数据取得，不得使用客户端自报的归属字段。表达式中不存在的属性或其他配置错误可能产生求值异常，不能作为允许访问处理。
- 只适用于通过 Spring Bean 代理调用的公开方法；同类自调用、手动 new 对象不能依赖该注解。可以同时使用 `@PreAuthorize("@pms.hasPermission(...)")`，资源检查与操作权限都必须通过。
- 不过滤列表、分页、统计或导出；这些操作应在数据库查询中限制数据范围。不支持异步、流式返回值的拥有者解析。
- 返回后检查不能撤销已经发生的写入、消息发送等副作用，修改、删除和退款应使用执行前鉴权及数据库条件约束。
- 拥有者相等不代表租户授权成立；多租户业务仍需独立限制租户范围，尤其不能仅比较租户内局部唯一的用户编号。

### 列表授权范围的 Passkey 与统一 CheckOwner

两种资源校验依据使用同一个注解，由表达式上下文确定时机，与 HTTP 动词或客户端是否携带凭证无关：

| 场景 | 用法 | 时机 |
| --- | --- | --- |
| 数据有明确的当前用户归属 | `@CheckOwner("returnObject.buyerUsername")` | 方法返回后检查拥有者，无需列表或 passkey |
| 无法直接判断用户与资源关系，依赖前置授权列表框定范围 | `@CheckOwner("#p0")` / `@CheckOwner("#p0.orderId")` | 方法执行前验证资源 passkey |

位置参数支持 `#p0`、`#a0`；命名参数依赖参数名可发现，并按实际声明 `@CheckOwner` 的接口或实现方法解析；实现方法可以使用不同的参数名。只支持同步单对象，入参资源编号支持非空 String 或整数（规范十进制表示），不支持集合。混合引用返回值与入参、常量或无法明确分类的表达式拒绝。两种模式失败不会互相降级。现有 `AnnotationTemplateExpressionDefaults` Bean 保留供其他组合注解使用，CheckOwner 本身改由统一 Advisor 执行。

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

接口声明 `getResourceId()`、`getPasskey()`、`setPasskey(...)`，辅助 resourceId 带 JsonIgnore，不额外出现在 JSON 中；DTO 自己的 orderId 正常输出。无需签发注解。**所有返回此类列表元素的入口必须已经完成资源授权**，不能在未授权公共列表复用 DTO；单个对象实现接口不自动签发。列表与详情 DTO 可以不同，后置归属模式不要求实现该接口。

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

所有实例使用相同签名配置；不要使用用户登录 JWT 的默认密钥，不要提交密钥。默认服务使用 HMAC-SHA256，将版本、可信 `authentication.name`、全局唯一资源编号和过期时间纳入明确字段编码；不包含租户、类型、action。应用可提供自己的 `PasskeyService` Bean 替换签发与验证，或提供 Clock Bean 替换时间源。服务契约同时继承公共签发 SPI，替换后签发与验证使用同一实例；自定义实现必须保留拒绝与身份校验约束。

现有 Web Starter 的 ControllerResponseAdvice 为授权列表签发，支持 Collection、PageVO、成功 Result 包装及 ResponseEntity 正文，混合列表只处理接口实现元素。关闭/替换该 Advice 后须自行接入签发；`macula.security.passkey.enabled=false`（默认）时跳过列表遍历与签发，正常返回，即使有自定义签发器也不会调用。启用时有候选列表但无有效签发器则拒绝，配置无效时默认服务启动失败。只有 Security Starter 而未接入 Web Advice 时不会自动签发，前置保护仍要求有效凭证。

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

前端携带 `X-Resource-Passkey` Header（推荐）或 URL Query 的 `passkey`。两处同值可用，重复、冲突、非法编码或空值拒绝；不读取表单同名字段，也不使用返回 DTO 中的凭证验证请求。缺失/无效凭证在执行前拒绝，不进入业务方法。接入现有异常 Advice 后，本功能的资源拒绝返回 HTTP 403 与 A0312，且不包含详情。

凭证在到期前可重复使用；业务授权变更不会立即撤销已签发凭证。编号必须全局唯一、表示稳定。PMS 继续负责按钮/操作权限，业务事务继续检查订单状态等条件；单资源凭证不能用于授权请求内的其他资源，也不能替代租户或其他业务边界控制。后置归属检查仍只能用于无副作用的查询。

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
