# Alibaba Provider 示例

该模块是 Alibaba 微服务链路的服务提供方，演示 Nacos 注册与配置刷新、JWT 资源服务器、SpringDoc、统一 OTLP 可观测性以及 OSS 配置。

OTLP Metrics、Traces、Logs 网络导出默认关闭，配置 Docker observability overlay 时开启。

## 启动

先启动 Nacos，再在仓库根目录执行：

```bash
mvn -pl macula-boot-examples/macula-example-alibaba-provider1 -am spring-boot:run
```

默认端口是 `5020`。可使用 `NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_USERNAME` 和 `NACOS_PASSWORD` 覆盖注册中心配置。

## 验证

`echo` 接口在本地配置中免鉴权，同时会读取可动态刷新的 `example.test`：

```bash
curl "http://127.0.0.1:5020/api/v1/provider1/echo?str=hello"
curl "http://127.0.0.1:5020/actuator/health"
```

Swagger UI 默认位于 `http://127.0.0.1:5020/swagger-ui/index.html`。`macula.oss` 下的 COS 值只是配置示例，调用真实存储前应使用环境配置覆盖。
