## 概述

基于腾讯云企业级微服务框架[TSF](https://cloud.tencent.com/document/product/649/13005)搭建的微服务开发平台。

## 组件坐标

```xml
<!-- 微服务模块依赖 -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-cloud-tsf</artifactId>
    <version>${macula.version}</version>
</dependency>

        <!-- 网关模块依赖 -->
<dependency>
<groupId>dev.macula.boot</groupId>
<artifactId>macula-boot-starter-cloud-tsf-scg</artifactId>
<version>${macula.version}</version>
</dependency>
```

## 使用配置

在bootstrap.yml中如下配置，主要是公共配置

```yaml
server:
  port: 7082

spring:
  profiles:
    # maven打包的时候指定profile，包括local,dev,test,staging,pet,prd，默认启用local
    active: @profile.active@
  application:
    name: macula-example-tsf
```

在项目文件application.yml中如下配置，默认是本地配置。也可以使用application-xxx.yml区分不同环境的配置。

```yaml
server:
  servlet:
    encoding:
      force: true

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://127.0.0.1:8000/oauth2/jwks        # 网关地址

seata:
  enabled: false

feign:
  httpclient:
    enabled: false
    max-connections: 200 # 线程池最大连接数，默认200
    time-to-live: 900 # 线程存活时间，单位秒，默认900
    connection-timeout: 2000  # 新建连接超时时间，单位ms, 默认2000
    follow-redirects: true # 是否允许重定向，默认true
    disable-ssl-validation: false # 是否禁止SSL检查， 默认false
    okhttp:
      read-timeout: 60s # 请求超时时间，Duration配置方式
  okhttp:
    enabled: true
  sentinel:
    enabled: false

macula:
  security:
    ignore-urls:
      - /api/v1/tsf/echo

logging:
  level:
    root: info
    dev.macula.example: debug
  file:
    name: ${user.home}/logs/${spring.application.name}/${spring.application.name}.log
```

当系统部署到TSF时，如果使用TSF的配置管理，建议将bootstrap.yml内容作为应用的公共配置，环境相关配置分别关联到不同环境的部署组。

## 核心功能

#### @EnableTsf注解

启动类需要添加@EnableTsf注解，替换@EnableDiscoveryClient

```java

@EnableTsf
@EnableFeignClients
@SpringBootApplication
public class MaculaExampleTsfApplication {
    public static void main(String[] args) {
        SpringApplication.run(MaculaExampleTsfApplication.class, args);
    }
}
```

其他和普通的spring-cloud没有太大不同，建议参考[官网说明](https://cloud.tencent.com/document/product/649/20261)

## 依赖引入

微服务模块引入：

```xml

<dependencies>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-feign</artifactId>
    </dependency>

    <dependency>
        <groupId>com.tencent.tsf</groupId>
        <artifactId>spring-cloud-tsf-starter</artifactId>
    </dependency>

</dependencies>
```

网关模块引入：

```xml

<dependencies>
    <dependency>
        <groupId>dev.macula.boot</groupId>
        <artifactId>macula-boot-starter-cloud-gateway</artifactId>
    </dependency>

    <dependency>
        <groupId>com.tencent.tsf</groupId>
        <artifactId>spring-cloud-tsf-msgw-scg</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- TSF：TSF属于腾讯云的企业版产品，具体日志见：https://cloud.tencent.com/document/product/649/85871