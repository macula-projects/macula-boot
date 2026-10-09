# Alibaba Consumer 示例

该模块演示通过 Nacos 发现 Alibaba provider，并使用 OpenFeign 调用服务、Sentinel fallback 处理降级、STOMP over WebSocket 发送消息。

模块使用统一 Observability Starter 和 Async Starter；OTLP 网络导出默认关闭，开关和端点配置在 `application.yml`，启用 Docker observability overlay 时开启。
回声接口会额外产生一条受管异步日志，用于验证 Trace 上下文能够跨线程传播。

## 启动

先启动 Nacos 和 `macula-example-alibaba-provider1`，再执行：

```bash
# 在仓库根目录先安装所需框架依赖，再只启动当前应用
mvn -pl macula-boot-examples/macula-example-alibaba-consumer -am install -DskipTests -Dgpg.skip=true -Pdeploy
mvn -f macula-boot-examples/macula-example-alibaba-consumer/pom.xml spring-boot:run
```

默认端口是 `5010`。local 使用 `NACOS_SERVER_PORT` 覆盖本机 Nacos 端口，docker / 共享环境使用 `NACOS_SERVER_ADDR` 覆盖完整地址；命名空间和认证使用 `NACOS_NAMESPACE`、`NACOS_USERNAME`、`NACOS_PASSWORD`。local 连接远程 Nacos 时直接覆盖 `spring.config.nacos.server-addr`。

## 验证

```bash
curl "http://127.0.0.1:5010/api/v1/consumer/echo/demo?str=hello"
```

WebSocket 演示页位于 `http://127.0.0.1:5010/`。通过网关连接时，页面默认使用 `ws://localhost:5000/websocket/websocket`。

`GapiService` 和 `IpaasService` 是带 AK/SK 拦截器的外部 Feign 示例，对应的账号、密钥和地址都应由实际环境覆盖；不验证该能力时无需调用相关端点。
