# ${rootArtifactId}

## 可观测性

所有可运行后端模块默认引入 `macula-boot-starter-observability`，提供 OTLP Metrics、Traces、Logs 以及 Logstash JSON 结构化日志。网络导出默认关闭；local/docker 的开关、采样率、endpoint 和资源属性直接维护在各模块 `application.yml`，不通过 deploy/Compose 环境变量注入，共享环境由对应 Nacos Data ID 提供。

Gateway 已启用 `spring.reactor.context-propagation=auto`。API-only、聚合 POM 和前端模块不会引入运行时可观测性依赖。

## 运行步骤

```shell
cp deploy/.env.example deploy/.env
./deploy/scripts/compose.sh config
./deploy/scripts/compose.sh up
```

默认模式启动 MySQL、Redis、Nacos 和幂等初始化任务。Service1 启动时由 Flyway 自动创建示例表，不再需要手工导入 SQL。Java 服务可在 IDE 中按 Service1/Basic/OpenAPI/Thirdparty、Admin BFF、Gateway 的顺序启动。
本地基础设施默认使用 `2xxxx` 宿主机端口，Gateway 使用 `6000`，Admin 使用 `5800`，不会占用 Macula Cloud 的标准端口；其他应用模块只在 Compose 网络内可达，不映射宿主机端口。
Admin 在 IDE 模式下使用 Vite 将同源 `/api` 代理到本机 Gateway `6000`，外部 IAM 与 OAuth 示例配置读取 `deploy/.env`；用户和菜单也经 `/api/admin/**` 进入 Admin BFF。Docker 模式才由 Nginx 在启动时生成浏览器 `config.js`。

完整容器模式使用：

```shell
./deploy/scripts/compose.sh up-apps
```

登录依赖已运行的 Macula Cloud Gateway、IAM 与 System。请先在 Cloud 中注册 `${rootArtifactId}-admin-bff` 应用和本地 demo OAuth client，再在 `deploy/.env` 配置浏览器使用的 IAM 地址、后端容器使用的 Cloud 地址、OAuth client 与示例账号。SPA 中的 client secret 不是安全秘密，不得填写生产凭据。详细端口、启动和重置说明见 [deploy/README.md](deploy/README.md)。
