# 将redis的psync事件转为rocketmq的消息

将redis的psync事件转为rocketmq的消息，可以用于更新redis后的增量落库，建议晚上有个全量校验，防止增量有问题

参考：

- [rocket-mq-externals](https://github.com/apache/rocketmq-externals/rocketmq-redis)
- [redis-replicator](https://github.com/leonchen83/redis-replicator)
