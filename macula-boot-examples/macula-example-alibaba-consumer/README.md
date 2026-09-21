# Alibaba Consumer 示例

该模块演示通过 Nacos 发现 Alibaba provider，并使用 OpenFeign 调用服务、Sentinel fallback 处理降级、STOMP over WebSocket 发送消息。

## 启动

先启动 Nacos 和 `macula-example-alibaba-provider1`，再执行：

```bash
mvn -pl macula-boot-examples/macula-example-alibaba-consumer -am spring-boot:run
```

默认端口是 `5010`。可使用 `NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_USERNAME` 和 `NACOS_PASSWORD` 覆盖 Nacos 配置。

## 验证

```bash
curl "http://127.0.0.1:5010/api/v1/consumer/echo/demo?str=hello"
```

WebSocket 演示页位于 `http://127.0.0.1:5010/`。通过网关连接时，页面默认使用 `ws://localhost:5000/websocket/websocket`。

`GapiService` 和 `IpaasService` 是带 AK/SK 拦截器的外部 Feign 示例，对应的账号、密钥和地址都应由实际环境覆盖；不验证该能力时无需调用相关端点。
