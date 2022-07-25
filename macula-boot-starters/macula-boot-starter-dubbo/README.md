# Dubbo模块

已经不建议使用Dubbo，该模块只适用于作为消费者调用dubbo接口

```yaml
dubbo:
  application:
    name: dubbo-springboot-demo-provider
  protocol:
    name: dubbo
    port: -1
  registry:
    id: zk-registry
    address: zookeeper://127.0.0.1:2181
  config-center:
    address: zookeeper://127.0.0.1:2181
  metadata-report:
    address: zookeeper://127.0.0.1:2181
```

具体可以参考 [Dubbo Samples](https://dubbo.apache.org/zh/docs3-v2/java-sdk/quick-start/spring-boot/)