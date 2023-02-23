# 接入Macula Cloud的System模块

通过接入system模块，让你的系统用户系统管理的功能。只需要给xxx-admin模块依赖，提供加载菜单的url

```yaml
macula:
  cloud:
      app-key: ecp                            # 接入到macula cloud的appkey
      secret-key: xxxxx                       # 接入到macula cloud的secretKey
      system:
        endpoint: http://localhost:9000/system  # macula cloud system的端点
```