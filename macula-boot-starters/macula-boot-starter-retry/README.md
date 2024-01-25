## 概述

Spring Retry用于解决分布式系统中的网络抖动、连接超时、服务端不可用等网络问题，通过重试机制来保证服务的可用性。在macula中，我们使用了Spring
Retry来实现重试机制，通过配置retry策略来达到重试的目的。

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-retry</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

## 核心功能

### @Retryable注解

需要在重试的代码中加入重试注解@Retryable
```java
@Service 
@Slf4j 
public class RetryRequestService { 
    @Autowired 
    private OtherSystemSpi otherSystemSpi; 
 
    @Retryable(value = RuntimeException.class, maxAttempts = 5, backoff = @Backoff(delay = 100)) 
    public String request(String param) { 
        double random = Math.random(); 
 log.info("请求进来了，随机值为：" + random); 
        if (random > 0.1) { 
            throw new RuntimeException("超时"); 
        } 
        return otherSystemSpi.request(param); 
    } 
}  

```

当然，我们这里写了个调皮的逻辑来模拟超时。如果随机值大于0.1则抛出一个RuntimeException异常。每次请求进来时都会输出日志。

我来解释一下@Retryable注解中的信息。

- value：指定发生的异常进行重试
- include：和value一样，默认空，当exclude也为空时，所有异常都重试
- exclude：指定异常不重试，默认空，当include也为空时，所有异常都重试
- maxAttemps：重试次数，默认3
- backoff：重试等待策略,下面会在@Backoff中介绍
- recover：表示重试次数到达最大重试次数后的回调方法

### @Backoff注解

- delay，重试之间的等待时间(以毫秒为单位)
- maxDelay，重试之间的最大等待时间(以毫秒为单位)
- multiplier，指定延迟的倍数
- delayExpression，重试之间的等待时间表达式
- maxDelayExpression，重试之间的最大等待时间表达式
- multiplierExpression，指定延迟的倍数表达式

### @Recover注解

有时，在达到最大重试次数后，我们可能希望执行一些特定的逻辑来处理重试失败的情况。@Recover注解可以用于定义在重试失败时执行的备用方法。
```java
@Retryable(maxAttempts = 3, value = {MyCustomException.class})
public void performSomeOperation() {
    // 重试逻辑需要执行的操作
}

@Recover
public void handleRecovery(MyCustomException exception) {
    // 备用逻辑，用于处理重试失败的情况
}
```

在上面的示例中，当达到最大重试次数后，handleRecovery()方法将被调用，并且传递给它的参数是触发重试失败的异常对象。

回调方法的参数可以可选地包括抛出的异常和（可选）传递给原始可重试方法的参数。

<strong>回调方法注意事项：</strong>

- 方法的返回值必须与@Retryable方法一致
- 方法的第一个参数，必须是Throwable类型的，建议是与@Retryable配置的异常一致
- 回调方法与重试方法写在同一个类里面

## 依赖引入

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-aop</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.retry</groupId>
        <artifactId>spring-retry</artifactId>
    </dependency>
</dependencies>
```


## 版权说明

- spring-retry：https://github.com/spring-projects/spring-retry/blob/master/LICENSE