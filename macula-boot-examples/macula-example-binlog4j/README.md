# Binlog4j 示例

该模块演示 `macula-boot-starter-binlog4j` 的 MySQL binlog 订阅。`UserEventHandler` 监听 `macula-system.sys_user` 表的新增、修改和删除事件，并将解析后的数据输出到控制台。

## 前置条件

- MySQL `127.0.0.1:3306`，开启 binlog，建议使用 `ROW` 格式。
- 订阅账号具有 replication 权限。
- Redis `127.0.0.1:6379`，用于集群模式的状态协调。
- 数据库 `macula-system` 中存在 `sys_user` 表。

## 配置与启动

`application.yml` 中的 `binlog4j.client-configs.master` 定义 MySQL 连接。本地默认用户是 `root`、密码为空，请按实际环境覆盖。

```bash
mvn -pl macula-boot-examples/macula-example-binlog4j -am spring-boot:run
```

启动后对 `sys_user` 执行 `INSERT`、`UPDATE` 或 `DELETE`，控制台应输出对应事件。如果复制示例监听其他表，同步修改 `@BinlogSubscriber` 的 `database` / `table` 和事件实体字段。
