## 概述

本模块是 `macula-cloud-system` 的客户端集成，提供菜单、当前用户查询及按钮权限校验，不包含系统管理服务端。

## 客户端接入

### 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-system</artifactId>
    <version>${macula.version}</version>
</dependency>
```

### 使用配置

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `macula.cloud.endpoint` | 无，必填 | 网关基础 URL，客户端自动追加 `/system` |
| `macula.cloud.app-key` | 无，必填 | 在平台登记的应用标识 |
| `macula.cloud.secret-key` | 无，必填 | HMAC 签名密钥，部署时注入 |

请求超时通过 `spring.cloud.openfeign.client.config.systemFeignClient.*` 配置，参见 [Feign](../macula-boot-starter-feign/README.md)。

```yaml
macula:
  cloud:
    endpoint: http://127.0.0.1:9000          # 网关地址
    app-key: example
    secret-key: example
```

### 核心功能

#### 对接macula-cloud-system

根据app-key对应的应用获取菜单和用户信息。SystemService是通过远程RPC访问macula-cloud-system。具体的controller如下：

```java
@Tag(name = "system模块对接接口")
@RestController
@RequiredArgsConstructor
public class SystemController {

    private final SystemService systemService;

    @Operation(summary = "获取登录用户信息")
    @GetMapping("/api/v1/users/me")
    public UserLoginVO getLoginUserInfo() {
        // 从macula-cloud获取用户信息
        UserLoginVO userLoginVO = systemService.getUseInfo();
        userLoginVO.setRoles(SecurityUtils.getRoles());
        return userLoginVO;
    }

    @Operation(summary = "路由列表")
    @GetMapping("/api/v1/menus/routes")
    public List<RouteVO> listRoutes() {
        return systemService.listRoutes();
    }
}
```

#### 提供按钮权限注解鉴权

```java
@PreAuthorize("@pms.hasPermission('sys:user:del')")
```

### 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-commons</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-springdoc</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-feign</artifactId>
    </dependency>
</dependencies>
```

## 服务端介绍

`macula-cloud-system` 需独立部署，并通过网关暴露 `/system` 路由；应用标识、密钥及用户权限由服务端维护。

## 版权说明

- system模块代码参考了youlai-mall，https://github.com/youlaitech/youlai-mall/blob/master/LICENSE
