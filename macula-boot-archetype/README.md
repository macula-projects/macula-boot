# Macula Boot Archetype

用于生成 Macula Boot 多模块微服务项目。

```shell
mvn archetype:generate \
    -DgroupId=dev.macula.samples \
    -DartifactId=macula-samples \
    -Dversion=1.0.0-SNAPSHOT \
    -DarchetypeArtifactId=macula-boot-archetype \
    -DarchetypeGroupId=dev.macula.boot \
    -DarchetypeVersion=6.1.0-SNAPSHOT \
    -Dgitignore=.gitignore \
    -DinteractiveMode=false
```

生成后先复制 `deploy/.env.example` 为 `deploy/.env`，再执行：

```shell
./deploy/scripts/compose.sh config
./deploy/scripts/compose.sh up
mvn test
```

默认只启动 MySQL、Redis、Nacos 和初始化任务；`up-apps` 会构建并启动完整应用。Admin 登录依赖已运行且已完成应用/OAuth client 注册的 Macula Cloud Gateway、IAM 与 System。
生成项目默认使用独立的 `2xxxx` 基础设施宿主机端口、`6000` Gateway 端口和 `5800` Admin 端口，可与使用标准端口的 Macula Cloud 同时运行；其他应用模块仅在 Compose 网络内可达。
