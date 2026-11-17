# Tencent Gateway 示例

该模块演示 `macula-boot-starter-cloud-tencent-scg`：通过 Polaris 发现 consumer，并由 Spring Cloud Gateway 转发请求。

模块使用统一 Observability Starter；OTLP 网络导出默认关闭，配置 Docker observability overlay 时开启。WebFlux 已启用 Reactor 自动上下文传播。

## 启动

启动 Polaris 和 `macula-example-tencent-consumer` 后执行：

```bash
# 在仓库根目录先安装所需框架依赖，再只启动当前应用
mvn -pl macula-boot-examples/macula-example-tencent-gateway -am install -DskipTests -Dgpg.skip=true -Pdeploy
mvn -f macula-boot-examples/macula-example-tencent-gateway/pom.xml spring-boot:run
```

默认端口为 `4000`，Polaris 地址为 `grpc://127.0.0.1:38091`，Spring Cloud Tencent 的 Nacos 兼容 discovery 地址为 `127.0.0.1:38849`。可通过 `POLARIS_SERVER_ADDR`、`POLARIS_NACOS_SERVER_ADDR` 和 `POLARIS_NAMESPACE` 覆盖。

## 验证

```bash
curl "http://127.0.0.1:4000/consumer/api/v1/consumer/echo"
```

`/consumer/**` 会去除一层前缀后转发到 `lb://macula-example-tencent-consumer`。OAuth2 client secret 是本地演示值，不得直接用于部署环境。

## 请求体与访问限制

请求体缓存默认上限 10 MiB，签名/加密请求超限返回 HTTP 413；使用 `MACULA_GATEWAY_MAX_CACHED_BODY_SIZE` 可调整。此上限约束现有缓存分支，不能替代入口并发及上传策略。

资源服务器应继续使用已配置的 JWK 等验证来源；仅依赖旧默认 HMAC Secret 的下游应用需显式配置至少 32 字节的 Secret。请求租户切换仅允许 ROOT，普通身份继续采用认证租户。
