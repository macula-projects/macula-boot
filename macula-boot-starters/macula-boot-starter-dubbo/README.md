# Macula Boot Starter Dubbo

### 使用Dubbo开发微服务

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

### Dubbo与Spring Cloud互通

具体示例可以参考[dubbo-samples-cloud-native](https://github.com/apache/dubbo-samples/blob/master/2-advanced/dubbo-samples-cloud-native/README.md)