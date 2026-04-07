# 中间件使用规范

> Redis、RocketMQ 等中间件使用规约。

## Redis 开发规范

### 键值设计

#### Key 名设计

1. **可读性和可管理性**：以业务名(或数据库名)为前缀(防止 key 冲突)，用冒号分隔
   - 示例：`业务名:表名:id` → `ugc:video:1`

2. **简洁性**：保证语义的前提下，控制 key 的长度
   - 简化前：`user:{uid}:friends:messages:{mid}`
   - 简化后：`u:{uid}:fr:m:{mid}`

3. **强制**：不要包含特殊字符（空格、换行、单双引号以及其他转义字符）

#### Value 设计

1. **强制**：拒绝 bigkey
   - string 类型控制在 10KB 以内
   - hash、list、set、zset 元素个数不要超过 5000
   - 非字符串的 bigkey，不要使用 del 删除，使用 hscan、sscan、zscan 方式渐进式删除

2. **推荐**：选择适合的数据类型
   - 反例：使用多个 string 分别存储用户的多个属性
   - 正例：使用一个 hash 存储用户所有属性，hmset user:1 name tom age 19 favor football

3. **推荐**：控制 key 的生命周期，redis 不是垃圾桶
   - 建议使用 expire 设置过期时间
   - 条件允许可以打散过期时间，防止集中过期

### 命令使用规范

1. **推荐**：O(N) 命令关注 N 的数量
   - 例如 hgetall、lrange、smembers、zrange、sinter 等并非不能使用，但需要明确 N 的值
   - 有遍历的需求可以使用 hscan、sscan、zscan 代替

2. **推荐**：禁止线上使用 keys、flushall、flushdb 等
   - 通过 redis 的 rename 机制禁掉命令
   - 或者使用 scan 的方式渐进式处理

3. **推荐**：合理使用 select
   - redis 的多数据库较弱，很多客户端支持较差
   - 多业务使用多数据库实际还是单线程处理，会有干扰

4. **推荐**：使用批量操作提高效率
   - 原生命令：例如 mget、mset
   - 非原生命令：可以使用 pipeline 提高效率
   - 注意控制一次批量操作的元素个数（建议 500 以内）

   两者区别：
   - 原生是原子操作，pipeline 是非原子操作
   - pipeline 可以打包不同的命令，原生做不到
   - pipeline 需要客户端和服务端同时支持

5. **建议**：Redis 事务功能较弱，不建议过多使用
   - Redis 的事务功能不支持回滚
   - 集群版本要求一次事务操作的 key 必须在一个 slot 上

6. **建议**：Redis 集群版本在使用 Lua 上有特殊要求
   - 所有 key 都应该由 KEYS 数组来传递
   - 所有 key 必须在 1 个 slot 上，否则直接返回 error

7. **建议**：必要情况下使用 monitor 命令时，要注意不要长时间使用

## RocketMQ 开发规范

### 命名规范

1. **【强制】** Topic 命名：`topic` + 下划线 + 业务名
   - 示例：订单领域 topic 命名为 `topic_order`

2. **【强制】** tag 命名：`tag` + 下划线 + 业务动作
   - 示例：订单创建的 tag 为 `tag_create`，订单关闭的 tag 为 `tag_close`

3. **【强制】** 生产者分组命名：`pg` + 下划线 + 业务名
   - 示例：订单创建生产者分组名为 `pg_order`

4. **【强制】** 消费者分组命名：`cg` + 下划线 + 业务名 + 下划线 + 订阅 topic 名称 + 下划线 + 订阅 tag 名称
   - 示例：订单服务 `topic_order` topic 的 `tag_create` tag，用户服务消费者分组命名为 `cg_user_order_create`

### 使用规范

#### 生产者

2.1 **【强制】** 一个领域服务只能有一个 topic

2.2 **【强制】** 领域服务发送消息时必须根据业务动作设置 tag

2.3 **【强制】** 在 Producer 发送消息时必须设置 keys

2.4 **【强制】** 消息发送成功或者失败要打印消息日志，务必要打印 SendResult 和 key 字段

2.5 **【推荐】** 消息发送失败后建议将消息存储到 db，然后由定时器类线程进行定时重试，确保消息达到 broker

2.6 **【推荐】** 对于可靠性要求不高的业务场景可以使用 oneway 消息

2.7 **【强制】** 新建生产者时必须指定生产者分组

#### 消费者

2.8 **【强制】** 新建消费者时必须指定消费者分组

2.9 **【强制】** 消息消费者无法避免消息重复，所以需要业务服务来保证消息消费幂等

2.10 **【推荐】** 为了提高消费并行度，可以在同一个 ConsumerGroup 下启动多个 Consumer 实例，或者通过修改 `ConsumeThreadMin` 和 `consumeThreadMax` 来提高单个 Consumer 的并行消费能力

2.11 **【推荐】** 为了增加业务吞吐量，可以通过设置 consumer 的 `consumeMessageBatchMaxSize` 来批量消费消息

2.12 **【推荐】** 发生消息堆积时，如果业务对数据要求不高时，可以选择丢弃不重要的消息

2.13 **【推荐】** 如果消息量较少，建议在消费入口打印消息、消费耗时等信息，方便后续排查问题

### 申请规范

3.1 **【强制】** 在新建 topic 时必须进行申请，评审通过方可使用

## 参考资料

- [阿里云 Redis 开发规范](https://developer.aliyun.com/article/531067)
- [RocketMQ 最佳实践](https://github.com/apache/rocketmq/blob/master/docs/cn/best_practice.md)
