## 概述

binlog4j是一个读取MySQL binlog的组件，以slave的方式接入mysql，将binlog转为事件。

## 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-binlog4j</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

先开启 MySQL ROW 格式 binlog 并授予专用账号必要的复制权限；使用持久化或集群选举时还需可用的 Redis。

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
binlog4j:
  client-configs:
    master:
      host: 127.0.0.1
      username: ${BINLOG_USERNAME}
      password: ${BINLOG_PASSWORD}
      server-id: 1990
      persistence: true
```

每个客户端独立配置，前缀为 `binlog4j.client-configs.<名称>`，名称需与 `@BinlogSubscriber.clientName` 一致。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `host` / `username` / `password` | 未设置 | MySQL 地址及账号 |
| `port` | `3306` | MySQL 端口 |
| `server-id` | `0` | 应显式指定复制客户端 ID，避免与其他复制客户端冲突 |
| `mode` | `standalone` | `cluster` 使用集群主备模式 |
| `persistence` | `false` | 将消费位点持久化到 Redis |
| `inaugural` | `false` | 为 `true` 时跳过恢复历史位点，勿作为常规重启配置 |
| `keep-alive` | `true` | 保持连接 |
| `keep-alive-interval` | `60000` | 保活间隔，毫秒 |
| `connect-timeout` | `3000` | 建连超时，毫秒 |
| `heartbeat-interval` | `5000` | 心跳间隔，毫秒 |
| `time-offset` | `0` | Date 类型值的时间偏移，毫秒 |
| `gtid-mode` | `false` | 使用 GTID 复制 |
| `gtid-purged` | `true` | GTID 集为空时读取已清理 GTID 信息 |
| `gtid-set-default` | 未设置 | 无缓存 GTID 位点时的起始集合 |

时间数值不要加 Java 的 `L` 后缀；GTID 模式须与 MySQL 服务端配置匹配。

## 核心功能

### 订阅binlog事件

```java
import tools.jackson.databind.json.JsonMapper;

@BinlogSubscriber(clientName = "master", database = "macula-system", table ="sys_user")
public class UserEventHandler implements IBinlogEventHandler<User> {

    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();

    @Override
    public void onInsert(User target) {
        System.out.println("插入数据：" + JSON_MAPPER.writeValueAsString(target));
    }

    @Override
    public void onUpdate(User source, User target) {
        System.out.println("修改数据:" + JSON_MAPPER.writeValueAsString(target));
    }

    @Override
    public void onDelete(User target) {
        System.out.println("删除数据");
    }
}

@Data
public class User {
    private Long id;
    private String username;
    private String nickname;
    private String mobile;
    private Integer gender;
    private String avatar;
    private String password;
    private String email;
    private Integer status;
    private Long deptId;
}
```

组件使用独立的 Jackson 3 Mapper，默认将下划线列名映射到驼峰属性，例如 `dept_id → deptId`，无需实体类声明命名策略。没有对应属性的列会忽略；无法转换的字段值会抛出异常。

特殊列名可通过 `com.fasterxml.jackson.annotation.JsonProperty` 显式映射，例如 `@JsonProperty("user_no") private String account;`。原有 fastjson2 的 `@JSONType`、`@JSONField` 和自定义反序列化器不再生效，升级时须迁移为 Jackson 注解或对应实现。日期、枚举等自定义转换应针对业务实体验证。

Redis 位点使用另一个独立 Mapper，保留 `serverId`、`position`、`filename`、`gtidSet` 字段名和原 Redis key，可读取历史位点 JSON。组件不修改应用全局 Mapper；位点 JSON 字段顺序和 null 输出不作为兼容性约定。

### 集群模式说明

同一个应用多个实例时要启用集群模式，serverId同一个应用保持一致，不同应用千万不要重复。否则前面的应用会被踢出。同一时刻同一个serverId只能有一个实例与MySQL连接。

本模块的高可用原理是通过redis锁，集群中某个实例先连上mysql后，后hold
redis锁，其他实例会单独一个线程等待锁，如果前面的实力出现异常宕机，redis锁释放，则其他实例会自动重连，并且从redis恢复binlog的postion，从而实现高可用。

## 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>
    <dependency>
        <groupId>tools.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid</artifactId>
    </dependency>
    <dependency>
        <groupId>io.debezium</groupId>
        <artifactId>mysql-binlog-connector-java</artifactId>
    </dependency>
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 版权说明

本代码沿用[binlog4j](https://github.com/dromara/binlog4j)代码，做了适配修改。

- binlog4j：https://github.com/dromara/binlog4j/blob/master/LICENSE
- mysql-binlog-connector-java: https://github.com/osheroff/mysql-binlog-connector-java
