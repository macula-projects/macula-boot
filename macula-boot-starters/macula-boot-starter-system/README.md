## 概述

macula-cloud-system主要提供系统管理、权限、角色等功能，并提供连接macula-cloud-system应用的能力，生成获取菜单、获取当前用户的Controller。

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

```yaml
macula:
  cloud:
    endpoint: http://127.0.0.1:9000					# 网关地址
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

## 版权说明

- system模块代码参考了youlai-mall，https://github.com/youlaitech/youlai-mall/blob/master/LICENSE