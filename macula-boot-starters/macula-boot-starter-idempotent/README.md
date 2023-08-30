## 概述

该模块对原有 [idempotent](https://github.com/it4alla/idempotent) 的代码进行了重构和功能增强。非常感谢idempotent 作者的分享。

## 组件坐标

```xml

<dependency>
  <groupId>dev.macula.boot</groupId>
  <artifactId>macula-boot-starter-idempotent</artifactId>
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

### 原理

1. 请求开始前，根据key查询 查到结果：报错。 未查到结果：存入key-value-expireTime key=ip+url+args；
2. 请求结束后，直接删除key，不管key是否存在，直接删除。是否删除，可配置；
3. expireTime过期时间，防止一个请求卡死，会一直阻塞，超过过期时间，自动删除。过期时间要大于业务执行时间，需要大概评估下;
4. 此方案直接切的是接口请求层面；
5. 过期时间需要大于业务执行时间，否则业务请求1进来还在执行中，前端未做遮罩，或者用户跳转页面后再回来做重复请求2，在业务层面上看，结果依旧是不符合预期的。
6. 建议delKey = false。即使业务执行完，也不删除key，强制锁expireTime的时间。预防5的情况发生；
7. 实现思路：同一个请求ip和接口，相同参数的请求，在expireTime内多次请求，只允许成功一次；
8. 页面做遮罩，数据库层面的唯一索引，先查询再添加，等处理方式应该都处理下；
9. 此注解只用于幂等，不用于锁，100个并发这种压测，会出现问题，在这种场景下也没有意义，实际中用户也不会出现1s或者3s内手动发送了50个或者100个重复请求,或者弱网下有100个重复请求；

### 接口设置注解

```java
public class DemoController {
    @Idempotent(key = "#demo.username", expireTime = 3, info = "请勿重复查询")
    @GetMapping("/test")
    public String test(Demo demo) {
        return "success";
    }
}
```

### @idempotent 配置详细说明

```java

@Inherited
@Target(ElementType.METHOD)
@Retention(value = RetentionPolicy.RUNTIME)
public @interface Idempotent {

  /**
   * <p>
   * 如果是实体类的话,默认拦截不会生效. objects.toString()会返回不同地址.
   * </p>
   * 幂等操作的唯一标识，使用spring el表达式 用#来引用方法参数
   *
   * @return Spring-EL expression
   */
  String key() default "";

  /**
   * 有效期 默认：1 有效期要大于程序执行时间，否则请求还是可能会进来
   *
   * @return expireTime
   */
  int expireTime() default 1;

  /**
   * 时间单位 默认：s
   *
   * @return TimeUnit
   */
  TimeUnit timeUnit() default TimeUnit.SECONDS;

  /**
   * 提示信息，可自定义
   *
   * @return String
   */
  String info() default "重复请求，请稍后重试";

  /**
   * 是否在业务完成后删除key true:删除 false:不删除
   *
   * @return boolean
   */
  boolean delKey() default false;
}
```

## 依赖引入

```xml

<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
  </dependency>
  <dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-redis</artifactId>
  </dependency>
</dependencies>
```

## 版权说明

- 原始代码1：https://github.com/it4alla/idempotent/blob/dev/LICENSE
- 原始代码2：https://github.com/pig-mesh/idempotent-spring-boot-starter/blob/master/LICENSE

