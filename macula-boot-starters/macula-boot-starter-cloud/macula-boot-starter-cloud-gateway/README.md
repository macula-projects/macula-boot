# macula-boot-starter-gateway

网关服务模块，给每个平台应用依赖用的

### 将oauth2的token转换为JWT传递给微服务，微服务通过JWT获取用户信息

### 添加了安全处理，根据用户的角色和URL所需角色对比，控制URL权限

### 接口的加解密处理

```yaml
macula:
  gateway:
    crypto-switch: true           # 接口加解密全局开关，默认true
    force-crypto: false           # 是否强制校验接口要不要加解密，默认false
    crypto-urls: # 需要加解密的URL，通过/gateway/crypto/urls获取
      - /system/xxx/**
      - /mall/api/v1/xxx/**
    security:
      ignore-urls: /usr/xxx,/bbb/xxx  # 忽略认证的路径，Ant Path格式
      only-auth-urls: /usr/xx, /bbb/xxx # 仅需认证无需鉴权的路径
      jwt-secret: xxx  # JWT的签名密钥
spring:
  redis:
    xxx         # 通过redis获取URL-ROLES对应数据
```