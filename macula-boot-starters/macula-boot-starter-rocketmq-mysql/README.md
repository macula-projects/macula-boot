# 将mysql的binlog转为rocketmq的消息

基于MySQL的Slave协议，订阅binlog，发出数据修改事件，可以用于缓存更新或者清除，异步业务处理。

参考：

- [rocketmq-externals](https://github.com/apache/rocketmq-externals/rocketmq-mysql)
- [mysql-binlog-connector-java](https://github.com/osheroff/mysql-binlog-connector-java)