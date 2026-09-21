# Tencent Provider 示例

该模块是 Tencent 微服务链路的服务提供方，演示 Polaris 服务注册、JWT 资源服务器、SpringDoc、Prometheus 和 OSS 配置。

## 启动

```bash
mvn -pl macula-boot-examples/macula-example-tencent-provider -am spring-boot:run
```

默认端口为 `4020`，Polaris 地址为 `grpc://127.0.0.1:8091`。可通过 `POLARIS_SERVER_ADDR` 和 `POLARIS_NAMESPACE` 覆盖。

## 验证

```bash
curl "http://127.0.0.1:4020/api/v1/provider1/echo?str=hello"
curl "http://127.0.0.1:4020/actuator/health"
```

Swagger UI 默认位于 `http://127.0.0.1:4020/swagger-ui/index.html`。`macula.oss` 下的 COS 参数仅用于展示配置结构，真实访问凭据应从环境注入。
