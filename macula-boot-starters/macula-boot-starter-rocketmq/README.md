## 概述

该模块主要依赖阿里开源的RocketMQ。[RocketMQ](https://github.com/apache/rocketmq-spring/wiki/%E7%94%A8%E6%88%B7%E6%89%8B%E5%86%8C)
支持同步、异步发送消息，半事务消息等，具体可以参考官方文档。

## 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-rocketmq</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 核心功能

### 消息序列化

Macula 的消息转换链使用 Jackson 3，保留字节数组、UTF-8 字符串和 JSON 消息支持。JSON 内置 Java 时间类型支持，日期仍输出 ISO 字符串，长整数仍输出数字；未知字段和基本类型的 `null` 值沿用历史兼容行为。转换链不再使用 Jackson 2 或 Fastjson 回退，自定义序列化器须使用 `tools.jackson` API。

RocketMQ Spring 2.3.4 自身初始化仍依赖 Jackson 2，因此保留第三方传递依赖。不要因 Macula 已使用 Jackson 3 而排除这些依赖。

### RocketMQ最佳实践

{{% alert title="提示" color="primary" %}}

[最佳实践](https://mp.weixin.qq.com/s/Rxzo584le5XzUuI71S9nJg)

{{% /alert %}}

### 生产者配置建议

```yaml
rocketmq:
  name-server: localhost:9876
  producer:
    namespace: ${spring.profiles.active} # 建议与环境保持一致${spring.profiles.active}
    group: ${spring.application.name}    # 建议采用${spring.application.name}
    sendMessageTimeout: 300000
    enableMsgTrace: true                 # 消息轨迹开启，根据需要来决定

demo:
  rocketmq:
    stringRequestTopic: stringRequestTopic:tagA
```

```java
public class Test {
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Value("${demo.rocketmq.stringRequestTopic}")
    private String stringRequestTopic;   // 按照最佳实践，topic一个应用保持一个topic，topic:tag，通过tag区分业务

    SendResult sendResult = rocketMQTemplate.syncSend(stringRequestTopic, "hello world");
}
```

### 消费者配置建议

#### Push模式

Push的namespace、topic等都是配置在@RocketMQMessageListener注解里面的

```yaml
rocketmq:
  name-server: localhost:9876
demo:
  rocketmq:
    stringRequestTopic: stringRequestTopic
    taga: TagA
```

```java
@SpringBootApplication
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

    @Slf4j
    @Service
    @RocketMQMessageListener(topic = "${demo.rocketmq.stringRequestTopic}", selectorExpression = "${deom.rocketmq.taga}",
            namespace = "${spring.profiles.active}", consumerGroup = "${spring.application.name}")
    public class MyConsumer1 implements RocketMQListener<String> {
        public void onMessage(String message) {
            log.info("received message: {}", message);
        }
    }

    @Slf4j
    @Service
    @RocketMQMessageListener(topic = "test-topic-2", consumerGroup = "my-consumer_test-topic-2")
    public class MyConsumer2 implements RocketMQListener<OrderPaidEvent> {
        public void onMessage(OrderPaidEvent orderPaidEvent) {
            log.info("received orderPaidEvent: {}", orderPaidEvent);
        }
    }
}
```

#### Pull模式

**从RocketMQ Spring 2.2.0开始，RocketMQ Srping支持Pull模式消费，自己单线程或者多线程获取消息，实时性不高**

```yaml
rocketmq:
  name-server: localhost:9876
  consumer: # 只用于pull模式
    namespace: ${spring.profiles.active} # 建议与环境保持一致${spring.profiles.active}
    group: ${spring.application.name}    # 建议采用${spring.application.name}
    topic: stringRequestTopic
    selectorExpression: tagA
    enableMsgTrace: true                 # 消息轨迹开启，根据需要来决定
```

```java
@SpringBootApplication
public class ConsumerApplication implements CommandLineRunner {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    @Resource(name = "extRocketMQTemplate")
    private RocketMQTemplate extRocketMQTemplate;

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        //This is an example of pull consumer using rocketMQTemplate.
        List<String> messages = rocketMQTemplate.receive(String.class);
        System.out.printf("receive from rocketMQTemplate, messages=%s %n", messages);

        //This is an example of pull consumer using extRocketMQTemplate.
        messages = extRocketMQTemplate.receive(String.class);
        System.out.printf("receive from extRocketMQTemplate, messages=%s %n", messages);
    }
}
```

#### 事务消息

```java
@SpringBootApplication
public class ProducerApplication implements CommandLineRunner{
    @Resource
    private RocketMQTemplate rocketMQTemplate;

    public static void main(String[] args){
        SpringApplication.run(ProducerApplication.class, args);
    }

    public void run(String... args) throws Exception {
        try {
            // Build a SpringMessage for sending in transaction
            Message msg = MessageBuilder.withPayload(..)...;
            // In sendMessageInTransaction(), the first parameter transaction name ("test")
            // must be same with the @RocketMQTransactionListener's member field 'transName'
            rocketMQTemplate.sendMessageInTransaction("test-topic", msg, null);
        } catch (MQClientException e) {
            e.printStackTrace(System.out);
        }
    }

    // Define transaction listener with the annotation @RocketMQTransactionListener
    @RocketMQTransactionListener
    class TransactionListenerImpl implements RocketMQLocalTransactionListener {
          @Override
          public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
            // ... local transaction process, return bollback, commit or unknown
            return RocketMQLocalTransactionState.UNKNOWN;
          }

          @Override
          public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
            // ... check transaction status and return bollback, commit or unknown
            return RocketMQLocalTransactionState.COMMIT;
          }
    }
}
```

默认情况下，@RocketMQTranscationListenner只能有一个，不过可以指定rocketMQTemplateBeanName来定义多个Listener，通过@ExtRocketMQTemplateConfiguration扩展RocketMQTemplate

```java
@ExtRocketMQTemplateConfiguration
public class ExtRocketMQTemplate extends RocketMQTemplate {

}
```

{{% alert title="提示" color="primary" %}}

可以参考[rocketmq-samples](https://github.com/apache/rocketmq-spring/blob/master/rocketmq-spring-boot-samples/rocketmq-produce-demo/src/main/java/org/apache/rocketmq/samples/springboot/ProducerApplication.java)

{{% /alert %}}

#### Macula 额外扩展

灰度消息配置前缀为 `macula.rocketmq.gray`，均为布尔值；不启用时使用常规生产、消费链路。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `false` | 开启按灰度泳道发送、消费消息 |
| `gray-consume-main` | `false` | 灰度实例是否同时消费基线消息 |
| `main-consume-gray` | `false` | 基线实例是否同时消费灰度消息 |

- 引入TxMqMessage，通过发送TxMqMessage执行业务方法和检查方法
- 引入@TxMqExecute和@TxMqCheck标识业务方法和检查方法

```java
    private RocketMQTemplate rocketMQTemplate;

    private final String BIZ_NAME_ORDER = "BIZ_ORDER";

    private final String TOPIC_ORDER = "TOPIC_ORDER_TX";

    public OrderServiceImpl(RocketMQTemplate template) {
        this.rocketMQTemplate = template;
    }

    public void createOrderWithMq(OrderVo order) {
        TxMqMessage txMsg = new TxMqMessage(order, this.getClass(), BIZ_NAME_ORDER, order.getOrderNo());
        rocketMQTemplate.sendMessageInTransaction(TOPIC_ORDER, txMsg, new Object[] { order });
    }

    @Transactional
    @TxMqExecute(BIZ_NAME_ORDER)
    public void createOrder(OrderVo order) {
        System.out.println("=============" + order.getOrderNo());
    }

    @TxMqCheck(BIZ_NAME_ORDER)
    public boolean checkOrder(String orderNo) {
        System.out.println("order_no=" + orderNo);
        return true;
    }
```

## 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>org.apache.rocketmq</groupId>
        <artifactId>rocketmq-spring-boot-starter</artifactId>
    </dependency>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-commons</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- rocketmq：https://github.com/apache/rocketmq/blob/develop/LICENSE
