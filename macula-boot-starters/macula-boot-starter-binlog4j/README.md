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
```

## 核心功能

### 订阅binlog事件

```java
@BinlogSubscriber(clientName = "master", database = "macula-system", table ="sys_user")
public class UserEventHandler implements IBinlogEventHandler<User> {

    @Override
    public void onInsert(User target) {
        System.out.println("插入数据：" + JSON.toJSONString(target));
    }

    @Override
    public void onUpdate(User source, User target) {
        System.out.println("修改数据:" + JSON.toJSONString(target));
    }

    @Override
    public void onDelete(User target) {
        System.out.println("删除数据");
    }
}

@Data
@JSONType(naming = PropertyNamingStrategy.SnakeCase)
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

> 注意Entity要使用SnakeCase策略

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
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2</artifactId>
    </dependency>
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid</artifactId>
    </dependency>
    <dependency>
        <groupId>com.zendesk</groupId>
        <artifactId>mysql-binlog-connector-java</artifactId>
    </dependency>
    <dependency>
        <groupId>mysql</groupId>
        <artifactId>mysql-connector-java</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 版权说明

本代码沿用[binlog4j](https://github.com/dromara/binlog4j)代码，做了适配修改。

- binlog4j：https://github.com/dromara/binlog4j/blob/master/LICENSE
- mysql-binlog-connector-java: https://github.com/osheroff/mysql-binlog-connector-java