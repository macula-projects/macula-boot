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

```yaml
spring:
  kafka:
    bootstrap-servers: 197.168.25.196:9092        				# 指定kafka server的地址，集群配多个，中间，逗号隔开
    producer:
      batch-size: 1000																		# 批量发送的消息数量
      buffer-memory: 33554432															# 32MB的批处理缓冲区
      retries: 3																					# 重试次数    
    consumer:
      group-id: crm-user-service													# 默认消费者组
      auto-offset-reset: earliest													# 最早未被消费的offset
      max-poll-records: 4000															# 批量一次最大拉取数据量
      enable-auto-commit: true														# 是否自动提交
      auto-commit-interval: 1000													# 自动提交时间间隔，单位ms
```

## 核心功能

### 发送数据

```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class KafkaProducerTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    public void testSend(){
        for (int i = 0; i < 5000; i++) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("datekey", 20210610);
            map.put("userid", i);
            map.put("salaryAmount", i);
            //向kafka的big_data_topic主题推送数据
            kafkaTemplate.send("big_data_topic", JSONObject.toJSONString(map));
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
			concurrency: 3																	# 批消费并发量，小于或等于Topic的分区数
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