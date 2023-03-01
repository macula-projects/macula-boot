# Macula Boot Starter PowerJob

## 配置

```yaml
powerjob:
  worker:
    akka-port: 27777                                # akka 工作端口，可选，默认 27777
    app-name: ${spring.application.name}            # 接入应用名称，用于分组隔离，推荐填写 本 Java 项目名称
    server-address: 127.0.0.1:7700,127.0.0.1:7701   # 调度服务器地址，IP:Port 或 域名，多值逗号分隔
    store-strategy: disk                            # 持久化方式，可选，默认 disk
    max-result-length: 4096                         # 任务返回结果信息的最大长度，超过这个长度的信息会被截断，默认值 8192
    max-appended-wf-context-length: 4096            # 单个任务追加的工作流上下文最大长度，超过这个长度的会被直接丢弃，默认值 8192
```

## 日志单独输出

目前，powerjob-worker 并没有实现自己的 LogFactory（如果有需求的话请提 ISSUE，可以考虑实现），原因如下：

1. powerjob-worker 的日志基于 Slf4J 输出，即采用了基于门面设计模式的日志框架，宿主应用无论如何都可以搭起 Slf4J
   与实际的日志框架这座桥梁。
2. 减轻了部分开发工作量，不再需要实现自己的 LogFactory。

为此，为了顺利且友好地输出日志，请在日志配置文件（logback.xml/log4j2.xml/...）中为 powerjob-worker 单独进行日志配置，比如（logback
示例）：

```xml
<appender name="POWERJOB_WORKER_APPENDER" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>${LOG_PATH}/powerjob-worker.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <FileNamePattern>${LOG_PATH}/powerjob-worker.%d{yyyy-MM-dd}.log</FileNamePattern>
        <MaxHistory>7</MaxHistory>
    </rollingPolicy>
    <encoder class="ch.qos.logback.classic.encoder.PatternLayoutEncoder">
        <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{50} - %msg%n</pattern>
        <charset>UTF-8</charset>
    </encoder>
    <append>true</append>
</appender>

<logger name="tech.powerjob.worker" level="INFO" additivity="false">
    <appender-ref ref="POWERJOB_WORKER_APPENDER" />
</logger>
```

## 处理器的开发

[详见](https://www.yuque.com/powerjob/guidence/hczm7m)

### 单机处理器示例

```java
// 支持 SpringBean 的形式
@Component
public class BasicProcessorDemo implements BasicProcessor {

    @Resource
    private MysteryService mysteryService;

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        // 在线日志功能，可以直接在控制台查看任务日志，非常便捷
        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.info("BasicProcessorDemo start to process, current JobParams is {}.", context.getJobParams());
        
        // TaskContext为任务的上下文信息，包含了在控制台录入的任务元数据，常用字段为
        // jobParams（任务参数，在控制台录入），instanceParams（任务实例参数，通过 OpenAPI 触发的任务实例才可能存在该参数）

        // 进行实际处理...
        mysteryService.hasaki();

        // 返回结果，该结果会被持久化到数据库，在前端页面直接查看，极为方便
        return new ProcessResult(true, "result is xxx");
    }
}
```

## TODO

需要测试Server如果部署在k8s集群的话，暴露哪些端口