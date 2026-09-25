# ${rootArtifactId}

## 可观测性

所有可运行后端模块默认引入 `macula-boot-starter-observability`，提供 OTLP Metrics、Traces、Logs
以及 Logstash JSON 结构化日志。网络导出默认关闭，可通过
`OTEL_METRICS_EXPORT_ENABLED`、`OTEL_TRACES_EXPORT_ENABLED`、`OTEL_LOGS_EXPORT_ENABLED` 开启；
三个 endpoint 和 `service.namespace` 也可由同目录 `application.yml` 中列出的环境变量覆盖。

Gateway 已启用 `spring.reactor.context-propagation=auto`。API-only、聚合 POM 和前端模块不会引入运行时可观测性依赖。

## 运行步骤

- 找到${rootArtifactId}-service1/docs/${rootArtifactId}-service1.sql，创建数据库，导入SQL创建示例表
- 向Macula Cloud申请应用接入，应用名称是${rootArtifactId}-admin-bff，修改上述应用的配置
    ```yaml
    macula:
      cloud:
        endpoint: http://127.0.0.1:9000                   # macula cloud网关地址
        app-key: ${spring.application.name}
        secret-key: 待修改
  ```
- 向Macula IAM申请oauth2
  client，修改${rootArtifactId}-admin/src/views/common/login/components/passwordForm.vue，修改client_id和client_secret
    ```html
        var data = {
          username: this.form.user,
          password: this.form.password,
          grant_type: 'password',
          client_id: '待修改',
          client_secret: '待修改',
          scope: 'message.read message.write userinfo'
        }
    ``` 
