## 概述

macula-cloud-retry基于[easy-retry](https://www.easyretry.com/)
来解决分布式系统中的数据一致性问题。为了确保分布式服务的可用性和数据一致性，并防止由于网络抖动、连接超时等问题导致短时不可用的情况，根据"
墨菲定律"，在核心流程中增加重试和数据核对校验的动作成为提高系统鲁棒性常用的技术方案。同时我们也提供了starter用于接入macula-cloud-retry服务。

## 客户端接入

### 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-retry</artifactId>
    <version>${macula.version}</version>
</dependency>
```

### 使用配置

```yaml
easy-retry:
  server:
    # 服务端的名称，和host port配置互斥，这里通过注册中心获取到服务端ip和port
    name: macula-cloud-retry
      # 服务端的地址，若服务端集群部署则此处配置域名
      host: 127.0.0.1
      # 服务端netty的端口号
      port: 1788
  # 指定客户端IP，不配置则取本地IP
  host: 127.0.0.1
  # 指定客户端端口，不配置则取取本地端口
  port: 8080
  # 批量上报滑动窗口配置
  slidingWindow:
    # 窗口期单位
    chrono-unit: seconds
    # 窗口期时间长度
    duration: 10
    # 总量窗口期阈值
    total-threshold: 50
    # 窗口数量预警
    window-total-threshold: 150     
```

### 核心功能

#### HelloWorld

在SpringBoot的启动项上增加注解@EnableEasyRetry，这个启动类中写入的macula对应的是我们控制台中的组名称。

```java

@SpringBootApplication
@EnableEasyRetry(group = "macula")
@EnableDiscoveryClient
@EnableFeignClients
public class MaculaExampleRetryApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaculaExampleRetryApplication.class, args);
    }
}
```

随后我们来写一个最简单的Service服务应用Easy-Retry

```java

@Component
public class LocalRetryService {
    @Retryable(scene = "localRetry")
    public void localRetry() {
        System.out.println("local retry 方法开始执行");
        double i = 1 / 0;
    }
}
```

大家可以看到这段代码中我们添加了一个注解@Retryable(scene = "localRetry")
，在其中指定了参数值scene，这个scene对应着控制台中的场景，我们可以在这里理解为场景就是组下面的唯一标识。

#### Retryable注解

请参考[官网](https://www.easyretry.com/)

#### ExecutorMethodRegister注解

请参考[官网](https://www.easyretry.com/)

### 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>com.aizuda</groupId>
        <artifactId>easy-retry-client-core</artifactId>
        <exclusions>
            <exclusion>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-jdbc</artifactId>
            </exclusion>
        </exclusions>
    </dependency>

    <dependency>
        <groupId>org.springframework.cloud</groupId>
        <artifactId>spring-cloud-starter-loadbalancer</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

## 服务端介绍

### 简介

macula-cloud-retry基于[easy-retry](https://www.easyretry.com/)
来解决分布式系统中的数据一致性问题。为了确保分布式服务的可用性和数据一致性，并防止由于网络抖动、连接超时等问题导致短时不可用的情况，根据"
墨菲定律"，在核心流程中增加重试和数据核对校验的动作成为提高系统鲁棒性常用的技术方案。

`SpringRetry`和`GuavaRetry`是由Spring和Google开发的重试框架，用于解决短期系统异常或网络抖动引起的问题。这两个框架均采用基于内存的重试策略。
相比之下，`EasyRetry`是一种面向分布式系统数据一致性问题的解决方案，它以重试为核心，提供了一系列功能来处理异常数据和确保最终一致性。
SpringRetry和GuavaRetry确实无法提供数据最终一致性的保障。它们主要用于处理短期系统异常或网络抖动，尝试重新执行操作，但是在重试N次后异常仍为解决，可能会导致数据丢失。对于核心业务场景，数据丢失可能带来严重的问题。

具体介绍可以参考easy-retry[官网](https://www.easyretry.com)

### 服务部署

由于服务端在重试的时候需要回调客户端，所以要保证服务端能够通过客户端注册的IP回调客户端。如果是k8s部署的，需要将easy
retry服务端与应用部署在同一个集群里面。可以获取macula-cloud-retry代码修改相应配置部署至自己的应用集群中。

## 版权说明

- Easy-retry：https://github.com/aizuda/easy-retry/blob/master/LICENSE