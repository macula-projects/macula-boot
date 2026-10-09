## 概述

该模块依赖spring-kafka接入kafka服务。

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-kafka</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

本模块沿用 `spring.kafka.*`，无自有配置前缀。下列为示例值，不是所有属性的默认值；生产环境需按吞吐、重试及重复消费要求选择。

```yaml
spring:
  kafka:
    bootstrap-servers: 127.0.0.1:9092
    producer:
      batch-size: 16384
      buffer-memory: 33554432
    consumer:
      group-id: example-consumer
      auto-offset-reset: earliest
      max-poll-records: 500
      enable-auto-commit: false
```

| 属性（前缀 `spring.kafka`） | 说明 |
| --- | --- |
| `bootstrap-servers` | Broker 地址列表，多个地址以逗号分隔 |
| `producer.batch-size` / `buffer-memory` | 批次大小 / 缓冲区容量，单位字节，不是消息条数 |
| `producer.retries` / `acks` | 发送重试次数 / 确认策略 |
| `producer.key-serializer` / `value-serializer` | 与消息类型匹配的序列化器 |
| `consumer.group-id` | 消费者组标识 |
| `consumer.auto-offset-reset` | 无有效已提交位点时的起点策略；`earliest` 不代表每次从头消费 |
| `consumer.max-poll-records` | 每次 poll 最多返回的记录数 |
| `consumer.enable-auto-commit` / `auto-commit-interval` | 是否自动提交 / 自动提交间隔；关闭自动提交后由监听容器策略管理 |
| `consumer.key-deserializer` / `value-deserializer` | 与生产端匹配的反序列化器 |
| `listener.type` / `concurrency` / `ack-mode` | 单条或批量监听 / 并发数 / 提交模式 |

下面的字符串收发示例需配置 StringSerializer / StringDeserializer，或使用 Boot 对应默认配置。

## 核心功能

### 发送数据

```java
@SpringBootTest
public class KafkaProducerTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    public void testSend(){
        for (int i = 0; i < 5000; i++) {
            // 向 big_data_topic 发送字符串，业务 JSON 可由应用自己的 Mapper 构造
            kafkaTemplate.send("big_data_topic", "user-" + i);
        }
    }
}
```

### 消费数据

```java
@Component
public class BigDataTopicListener {

    private static final Logger log = LoggerFactory.getLogger(BigDataTopicListener.class);

    /**
     * 监听kafka数据
     * @param consumerRecords
     * @param ack
     */
    @KafkaListener(topics = {"big_data_topic"})
    public void consumer(ConsumerRecord<?, ?> consumerRecord) {
        log.info("收到bigData推送的数据'{}'", consumerRecord.toString());
        //...
        //db.save(consumerRecord);//插入或者更新数据
    }
}
```

### 批量消费模式

增加如下配置：

```yaml
spring:
  kafka:
    listener:
      type: BATCH
      concurrency: 3                                  # 批消费并发量，小于或等于Topic的分区数
```

## 依赖引入

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>

    <!-- optional - only needed when using kafka-streams -->
    <dependency>
        <groupId>org.apache.kafka</groupId>
        <artifactId>kafka-streams</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- spring-kafka：https://github.com/spring-projects/spring-kafka/blob/main/LICENCE.txt
