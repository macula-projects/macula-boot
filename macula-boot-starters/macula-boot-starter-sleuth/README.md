## 概览

本模块主要提供Tracing最终的配置和定制化的指引。可以选择zipkin、skywalking接入。

- macula-boot-starter-sleuth 接入zipkin

## 组件坐标

### Sleuth

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-sleuth</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

### Sleuth

可以参考[网上的文章](https://developer.aliyun.com/article/1203201)。

{{% alert title="提示" color="primary" %}}

如果没有引入spring-cloud-sleuth-zipkin这个jar文件，则trace信息会输出到控制台

{{% /alert %}}

#### 接入zipkin

配置zipkin地址，支持http、kafka、rabbitmq等上报方式，高并发时建议使用kafaka

```yaml
spring:
	zipkin:
    base-url: http://ip:port    #zipkin server 的地址
    sender:
      type: web    #如果ClassPath里没有kafka, active MQ, 默认是web的方式
    sleuth:
      sampler:
        probability: 1.0  #100%取样，生产环境应该低一点,用不着全部取出来
```

引入依赖

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>
```

可以参考[官网](https://docs.spring.io/spring-cloud-sleuth/docs/current/reference/html/)进一步了解详细配置

## 核心功能

### Sleuth

#### 日志中添加traceid

默认情况下，引入上述依赖后，日志格式中已经添加了traceid。通过TraceEnvironmentPostProcessor类自动修改logging.pattern.level。

#### 自定义埋点

请参考[官方文档](https://docs.spring.io/spring-cloud-sleuth/docs/current/reference/html/)

## 依赖引入

### Sleuth

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-sleuth</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-sleuth-zipkin</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 版权说明

- zipkin:  https://github.com/openzipkin/zipkin/blob/master/LICENSE