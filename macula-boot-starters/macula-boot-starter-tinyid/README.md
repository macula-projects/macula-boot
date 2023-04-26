# Macula Boot Starter TinyID

macula-cloud-tinyid服务的starter，通过如下配置使用：

```yaml
macula:
  cloud:
    tinyid:
      server: localhost:9000 # 可以通过逗号隔开多个服务端IP
      token: 0f673adf80504e2eaa552f5d791b644c
      connectTimeout: 5000
      readTimeout: 5000
```

# tinyid不经过网关，从性能和稳定性角度考虑，减少依赖环节。
