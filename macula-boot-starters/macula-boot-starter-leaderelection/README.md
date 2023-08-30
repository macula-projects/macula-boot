## 概述

*通过**redis**的**lock**做领导选举*。

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-leaderelection</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

### 配置redis

理论是支持 [macula-boot-starter-redis](../../框架基础/redis)全部配置

```yaml
spring:
  redis:
    host: 127.0.0.1
    port: 6379
```

## 核心功能

可以在应用中实现如下接口，当被选中时，onElected方法被触发，可以做一些标识或者leader应该做的事情。

```java
/**
 * {@code ElectionListener} 选中为领导触发该监听器
 *
 * @author rain
 * @since 2022/12/2 19:07
 */
public interface ElectionListener {

    /**
     * 被选中为领导时触发
     */
    void onElected();
}


@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Component
    public class Service {
        public Service(LeaderElection leaderElection) {
            leaderElection.addElectionListener(() -> System.out.println("master selected"));
        }
    }
}
```

## 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-redis</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- redisson：https://github.com/redisson/redisson/blob/master/LICENSE.txt



