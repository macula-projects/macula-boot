# Tencent Consumer 示例

该模块演示通过 Polaris 发现 `macula-example-tencent-provider`，再使用 OpenFeign 调用 provider 接口和 fallback 降级。

## 启动

先启动 Polaris 和 `macula-example-tencent-provider`，再执行：

```bash
mvn -pl macula-boot-examples/macula-example-tencent-consumer -am spring-boot:run
```

默认端口为 `4010`，Polaris 地址为 `grpc://127.0.0.1:8091`。可通过 `POLARIS_SERVER_ADDR` 和 `POLARIS_NAMESPACE` 覆盖。

## 验证

```bash
curl "http://127.0.0.1:4010/api/v1/consumer/echo"
```

GAPI、iPaaS 和网关 AK/SK 客户端用于展示 Feign 拦截器，它们不是本地 provider 调用链路的必需条件。
