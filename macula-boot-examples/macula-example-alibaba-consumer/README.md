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

local/docker 默认使用本地示例凭据 `nacos/nacos`，与 examples Docker 的 `mysql-init` 用户初始化一致。`NACOS_USERNAME`、`NACOS_PASSWORD` 只覆盖客户端凭据，不会自动创建用户或修改已有密码。若使用已有数据卷，请先执行 Docker README 中的用户初始化步骤；修改后需重启应用。

## 验证

```bash
curl "http://127.0.0.1:5010/api/v1/consumer/echo/demo?str=hello"
```

WebSocket 演示页位于 `http://127.0.0.1:5010/`。通过网关连接时，页面默认使用 `ws://localhost:5099/websocket/websocket`。

`GapiService` 和 `IpaasService` 是带 AK/SK 拦截器的外部 Feign 示例，对应的账号、密钥和地址都应由实际环境覆盖；不验证该能力时无需调用相关端点。

## 普通接口对照

`GET /api/v1/consumer/plain?str=...` 直接返回 `Plain ` 加输入文本，由统一响应处理包装为 Result，不调用 provider。local 示例将该精确路径加入匿名清单；网关入口为 `/consumer/api/v1/consumer/plain`，不加入加密或签名保护清单。

```bash
curl --get 'http://localhost:5010/api/v1/consumer/plain' --data-urlencode 'str=A&B+中文'
```

通过网关使用 `http://localhost:5099/consumer/api/v1/consumer/plain`。普通参数使用标准 Query 编码，网关不生成或要求加密签名头。此公开示例只回显调用方输入，不返回业务或身份信息。
