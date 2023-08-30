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