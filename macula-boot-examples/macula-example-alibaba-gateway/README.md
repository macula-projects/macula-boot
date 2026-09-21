# Alibaba Gateway 示例

该模块演示 `macula-boot-starter-cloud-alibaba-scg` 的网关接入：从 Nacos 发现 consumer，转发 HTTP 和 WebSocket 请求，并接入 Sentinel 与 OAuth2 资源服务器。

## 前置条件

- Nacos，默认地址 `127.0.0.1:8848`、命名空间 `MACULA5`。
- 已启动 `macula-example-alibaba-consumer`。
- HTTPS 使用 `src/main/resources/jwk/springboot.p12` 中的示例证书，仅供本地演示。

## 启动

```bash
mvn -pl macula-boot-examples/macula-example-alibaba-gateway -am spring-boot:run
```

可通过 `NACOS_SERVER_ADDR`、`NACOS_NAMESPACE`、`NACOS_USERNAME` 和 `NACOS_PASSWORD` 覆盖 Nacos 配置。应用同时监听 HTTP `5000` 和 HTTPS `5443`。

## 路由

| 入口 | 目标 |
| --- | --- |
| `/consumer/**` | `lb://macula-example-alibaba-consumer` |
| `/websocket/**` | `lb:ws://macula-example-alibaba-consumer` |

验证 consumer 回声接口：

```bash
curl "http://127.0.0.1:5000/consumer/api/v1/consumer/echo/demo?str=hello"
```

网关安全配置中包含演示用 client secret，部署前必须通过配置中心替换。
