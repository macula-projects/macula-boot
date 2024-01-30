## 概览

本模块主要提供Tracing最终的配置和定制化的指引。可以选择zipkin、skywalking接入。

- macula-boot-starter-skywalking 接入skywalking

## 组件坐标

### Skywalking

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-skywalking</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

### Skywaking

#### 接入Skywalking

接入Skywalking需要在启动命令行添加-javaagent参数，具体可参考[官网](https://skywalking.apache.org/docs/skywalking-java/v9.1.0/en/setup/service-agent/java-agent/readme/)

#### 将日志发送到Skywalking（不推荐）

添加macula-boot-starter-skywalking依赖，在你的logback-spring.xml的配置文件中，加入include

```xml
<include resource="org/springframework/boot/logging/logback/defaults.xml" />
<include resource="logback-skylog.xml" />
```

上述include的内容如下：

```xml
<included>
    <!-- 控制台输出 tid -->
    <appender name="stdout" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.mdc.TraceIdMDCPatternLogbackLayout">
                <Pattern>${CONSOLE_LOG_PATTERN}</Pattern>
            </layout>
        </encoder>
    </appender>

    <!-- skywalking 采集日志 -->
    <appender name="grpc-log" class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.log.GRPCLogClientAppender">
        <encoder class="ch.qos.logback.core.encoder.LayoutWrappingEncoder">
            <layout class="org.apache.skywalking.apm.toolkit.log.logback.v1.x.mdc.TraceIdMDCPatternLogbackLayout">
                <Pattern>${CONSOLE_LOG_PATTERN}</Pattern>
            </layout>
        </encoder>
    </appender>

    <root level="info">
        <appender-ref ref="stdout"/>
        <appender-ref ref="grpc-log"/>
    </root>
</included>
```

## 核心功能

### Skywalking

#### 日志中添加traceid

默认情况下，引入上述依赖后，日志格式中已经添加了traceid。

#### 自定义埋点

请参考[官方文档](https://skywalking.apache.org/docs/skywalking-java/v9.1.0/en/setup/service-agent/java-agent/application-toolkit-tracer/)

## 依赖引入

### Skywalking

```xml
<dependencies>
    <!-- skywalking 整合 logback -->
    <dependency>
        <groupId>org.apache.skywalking</groupId>
        <artifactId>apm-toolkit-logback-1.x</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.skywalking</groupId>
        <artifactId>apm-toolkit-trace</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- Skywalking:  https://github.com/apache/skywalking-java/blob/main/LICENSE
