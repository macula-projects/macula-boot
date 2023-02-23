# 基于Feign的微服务调用

## 请求头传递到下级微服务的拦截器

## 配置为okhttp包和连接池

```yaml
feign:
  httpclient:
    enabled: false
    max-connections: 200 # 线程池最大连接数，默认200
    time-to-live: 900 # 线程存活时间，单位秒，默认900
    connection-timeout: 2000  # 新建连接超时时间，单位ms, 默认2000
    follow-redirects: true # 是否允许重定向，默认true
    disable-ssl-validation: false # 是否禁止SSL检查， 默认false
    okhttp:
      read-timeout: 60s # 请求超时时间，Duration配置方式
  okhttp:
    enabled: true
```