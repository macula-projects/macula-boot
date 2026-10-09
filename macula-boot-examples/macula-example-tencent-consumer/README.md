# Tencent Consumer 示例

该模块演示通过 Polaris 发现 `macula-example-tencent-provider`，再使用 OpenFeign 调用 provider 接口和 fallback 降级。

模块使用统一 Observability Starter；OTLP Metrics、Traces、Logs 默认关闭，由 `application.yml` 的 `observability` Profile 统一开启。

## 启动

先启动 Polaris 和 `macula-example-tencent-provider`，再执行：

```bash
# 在仓库根目录先安装所需框架依赖，再只启动当前应用
mvn -pl macula-boot-examples/macula-example-tencent-consumer -am install -DskipTests -Dgpg.skip=true -Pdeploy
mvn -f macula-boot-examples/macula-example-tencent-consumer/pom.xml spring-boot:run
```

默认端口为 `4010`，Polaris 地址为 `grpc://127.0.0.1:38091`，Spring Cloud Tencent 的 Nacos 兼容 discovery 地址为 `127.0.0.1:38849`。可通过 `POLARIS_SERVER_ADDR`、`POLARIS_NACOS_SERVER_ADDR` 和 `POLARIS_NAMESPACE` 覆盖。

## 验证

```bash
curl "http://127.0.0.1:4010/api/v1/consumer/echo"
```

GAPI、iPaaS 和网关 AK/SK 客户端用于展示 Feign 拦截器，它们不是本地 provider 调用链路的必需条件。
