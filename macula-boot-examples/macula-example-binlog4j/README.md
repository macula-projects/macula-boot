# Binlog4j 示例

该模块演示 `macula-boot-starter-binlog4j` 的 MySQL binlog 订阅。`UserEventHandler` 监听 `macula-system.sys_user` 表的新增、修改和删除事件，并将解析后的数据输出到控制台。

## 前置条件

- MySQL `127.0.0.1:33306`，开启 binlog，建议使用 `ROW` 格式。
- 订阅账号具有 replication 权限。
- Redis `127.0.0.1:36379`，用于集群模式的状态协调。
- 数据库 `macula-system` 中存在 `sys_user` 表。

## 配置与启动

配置前缀 `binlog4j.client-configs.master` 定义 MySQL 连接，默认用户 `root`、密码为空，必须按实际环境覆盖。

| 属性 | 示例值 | 说明 |
| --- | --- | --- |
| `binlog4j.client-configs.master.host` / `port` | `127.0.0.1` / `33306` | MySQL 地址，端口可用 `MYSQL_PORT` 覆盖 |
| `binlog4j.client-configs.master.username` / `password` | 环境提供 | 复制账号及密码 |
| `binlog4j.client-configs.master.server-id` | `1991` | 复制客户端 ID，避免冲突 |
| `binlog4j.client-configs.master.mode` | `cluster` | 本示例使用 Redis 协调主备 |
| `binlog4j.client-configs.master.persistence` / `gtid-mode` | `true` / `true` | 持久化位点 / 使用 GTID，MySQL 需启用 GTID |
| `spring.data.redis.host` / `port` / `password` | 按环境配置 | Boot 4 Redis 连接属性 |

注意：当前示例文件仍写为旧的 `spring.redis.*`，运行前需在本地配置中改用 `spring.data.redis.*` 或通过启动参数覆盖。连接 Examples Compose 的 Redis 时使用端口 `36379` 和对应密码。完整配置见 [Binlog4j Starter](../../macula-boot-starters/macula-boot-starter-binlog4j/README.md)。

```bash
# 在仓库根目录先安装所需框架依赖，再只启动当前应用
mvn -pl macula-boot-examples/macula-example-binlog4j -am install -DskipTests -Dgpg.skip=true -Pdeploy
mvn -f macula-boot-examples/macula-example-binlog4j/pom.xml spring-boot:run
```

启动后对 `sys_user` 执行 `INSERT`、`UPDATE` 或 `DELETE`，控制台应输出对应事件。如果复制示例监听其他表，同步修改 `@BinlogSubscriber` 的 `database` / `table` 和事件实体字段。
