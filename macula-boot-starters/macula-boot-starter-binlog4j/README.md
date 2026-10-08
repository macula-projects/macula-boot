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

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379

binlog4j:
  client-configs:
    master: # clientID
      username: root            # mysql的用户
      password:                 # mysql的密码
      host: 127.0.0.1           # mysql的host
      port: 3306                # mysql的port
      serverId: 1990            # 本实例的serverId，不同应用不能重复
      persistence: true         # 是否将binlog位置持久化，默认false
      inaugural: false          # 是否是首次启动，默认false
      mode: cluster             # 启动模式，集群模式一个实例启动连接到mysql，其他standby，默认standalone
      keepAlive: true           # 是否保持连接，默认是true
      KeepAliveInterval: 60000L # 保持连接间隔，默认1分钟，单位毫秒
      connectTimeout: 3000L     # 连接超时时间，默认3秒，单位毫秒
      heartbeatInterval: 6000L  # 发送心跳间隔，默认6秒，单位毫秒  
      gtidMode: true            # 开启GTID模式，默认为false
      gtidPurged: true          # 当GTID SET为“”时，是否获取最近的purged的GTID SET，默认为true
      gtidSetDefault: "xxx"     # 设置开始消费的GTID位置
```

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
