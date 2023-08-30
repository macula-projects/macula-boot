## 概述

macula-cloud-sentinel基于官方的sentinel-dashboard并引入nacos作为数据源的服务治理管理平台。在macula-boot-starter-cloud-alibaba中已经引入了sentinel依赖包。只需通过配置打开即可。

基于macula-boot-starter-cloud-alibaba微服务应用才可以使用该服务。

基于macula-boot-starter-cloud-tencent的微服务应用可以使用[polarismesh](https://polarismesh.cn)服务治理平台。

基于macula-boot-starter-cloud-tsf的微服务应用可以直接使用腾讯云的TSF平台。

## 客户端使用

### 使用配置

macula平台默认基于nacos数据源承接Sentinel的动态配置

```yaml
spring:
  cloud:
    sentinel:
      #启动后马上初始化，而不是等有流量有再初始化。否则会提示：Runtime port not initialized, won't send heartbeat
      eager: true
      transport:
        # 控制台地址
        dashboard: localhost:8080
        # 客户端监控API的端口，默认8719，与Sentinel控制台做交互。有规则变化会把规则数据push给这个Http Server接收,然后注册到sentinel中
        port: 8719
      # Sentinel Nacos数据源配置，Nacos中的规则会⾃动同步到sentinel控制台的流控规则中
      # com.alibaba.cloud.sentinel.SentinelProperties.datasource
      # 配置了数据源后，在nacos修改中会自动同步到sentinel
      # ⾃定义数据源名,随意不重复即可；可多个
      # The following values are valid:
      # AUTHORITY,DEGRADE,FLOW,GW_API_GROUP,GW_FLOW,PARAM_FLOW,SYSTEM
      datasource:
        flow:
          # 指定数据源类型，要与服务端使用的nacos保持一致
          nacos:
            server-addr: 127.0.0.1:8848
            data-id: ${spring.application.name}-flow-rules
            namespace: SENTINEL
            # 默认分组：DEFAULT_GROUP
            # group-id: SENTINEL_GROUP
            data-type: json
            # 限流规则类型
            rule-type: flow
        degrade:
          nacos:
            server-addr: 127.0.0.1:8848
            data-id: ${spring.application.name}-degrade-rules
            namespace: SENTINEL
            # 默认分组：DEFAULT_GROUP
            # group-id: SENTINEL_GROUP
            data-type: json
            # 降级规则类型
            rule-type: degrade
```

更多配置项见：[官网](https://sca.aliyun.com/zh-cn/docs/2022.0.0.0/user-guide/sentinel/advanced-guide#%E6%9B%B4%E5%A4%9A%E9%85%8D%E7%BD%AE%E9%A1%B9)

规则类型包括：

```java
FLOW("flow",FlowRule.class),
    DEGRADE("degrade",DegradeRule.class),
    PARAM_FLOW("param-flow",ParamFlowRule.class),
    SYSTEM("system",SystemRule.class),
    AUTHORITY("authority",AuthorityRule.class),
    GW_FLOW("gw-flow","com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule"),
    GW_API_GROUP("gw-api-group","com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition");
```

macula-cloud-sentinel默认集成的规则data-id包括如下，实际配置要加上${spring.application.name}：

```java
public static final String FLOW_DATA_ID_POSTFIX="-flow-rules";
public static final String DEGRADE_DATA_ID_POSTFIX="-degrade-rules";
public static final String SYSTEM_DATA_ID_POSTFIX="-system-rules";
public static final String PARAM_FLOW_DATA_ID_POSTFIX="-param-flow-rules";
public static final String AUTHORITY_DATA_ID_POSTFIX="-authority-rules";
public static final String GATEWAY_FLOW_DATA_ID_POSTFIX="-gateway-flow-rules";
public static final String GATEWAY_API_GROUP_DATA_ID_POSTFIX="-gateway-api-group-rules";
```

### 核心功能

#### @SentinelResource注解

```java

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(ServiceApplication.class, args);
    }
}

@RestController
public class TestController {

    @GetMapping(value = "/hello")
    @SentinelResource("hello")
    public String hello() {
        return "Hello Sentinel";
    }
}
```

@SentinelResource 注解用来标识资源是否被限流、降级。上述例子上该注解的属性 hello 表示资源名。 @SentinelResource
还提供了其它额外的属性如 blockHandler，blockHandlerClass，fallback
用于表示限流或降级的操作，更多内容可以参考 [Sentinel注解支持](https://github.com/alibaba/Sentinel/wiki/注解支持)
。具体的规则需要在Sentinel Dashboard中配置

#### 客户端支持

Spring Cloud Alibaba 提供的 Sentinel 有关 Starter 提供了对 Spring Cloud 生态中如 OpenFeign、RestTemplate 等主流的客户端组件的适配支持。

##### OpenFeign（熔断）

spring-cloud-starter-alibaba-sentinel 适配了 [OpenFeign](https://github.com/OpenFeign/feign) 组件。如果想使用，除了引入必要的
Starter 依赖外还需要在配置文件打开 sentinel 对 feign 的支持：==feign.sentinel.enabled=true==。

这是一个 FeignClient 的简单使用示例：

```java

@FeignClient(name = "service-provider", fallback = EchoServiceFallback.class, configuration = FeignConfiguration.class)
public interface EchoService {
    @GetMapping(value = "/echo/{str}")
    String echo(@PathVariable("str") String str);
}

class FeignConfiguration {
    @Bean
    public EchoServiceFallback echoServiceFallback() {
        return new EchoServiceFallback();
    }
}

class EchoServiceFallback implements EchoService {
    @Override
    public String echo(@PathVariable("str") String str) {
        return "echo fallback";
    }
}
```

NOTE: Feign 对应的接口中的资源名策略定义：httpmethod:protocol://requesturl。@FeignClient 注解中的所有属性，Sentinel 都做了兼容。

EchoService 接口中方法 echo 对应的资源名为 GET:http://service-provider/echo/{str}。

##### RestTemplate（熔断）

spring-cloud-starter-alibaba-sentinel 支持对 RestTemplate 的服务调用使用 Sentinel 进行保护，在构造 RestTemplate
bean的时候需要加上 @SentinelRestTemplate 注解。

```java
@Bean @SentinelRestTemplate(blockHandler = "handleException",
    blockHandlerClass = ExceptionUtil.class) public RestTemplate restTemplate(){
    return new RestTemplate();
    }
```

@SentinelRestTemplate 注解的属性支持限流(blockHandler, blockHandlerClass)和降级(fallback, fallbackClass)的处理。

其中 blockHandler 或 fallback 属性对应的方法必须是对应 blockHandlerClass 或 fallbackClass 属性中的静态方法。

该方法的参数跟返回值跟 org.springframework.http.client.ClientHttpRequestInterceptor#interceptor 方法一致，其中参数多出了一个
BlockException 参数用于获取 Sentinel 捕获的异常。

比如上述 @SentinelRestTemplate 注解中 ExceptionUtil 的 handleException 属性对应的方法声明如下：

```java
public class ExceptionUtil {
    public static ClientHttpResponse handleException(HttpRequest request, byte[] body,
        ClientHttpRequestExecution execution, BlockException exception) {
        ...
    }
}
```

NOTE: 应用启动的时候会检查 @SentinelRestTemplate 注解对应的限流或降级方法是否存在，如不存在会抛出异常

@SentinelRestTemplate 注解的限流(blockHandler, blockHandlerClass)和降级(fallback, fallbackClass)属性不强制填写。

当使用 RestTemplate 调用被 Sentinel 熔断后，会返回 RestTemplate request block by sentinel 信息，或者也可以编写对应的方法自行处理返回信息。这里提供了
SentinelClientHttpResponse 用于构造返回信息。

Sentinel RestTemplate 限流的资源规则提供两种粒度：

- httpmethod:schema://host:port/path：协议、主机、端口和路径
- httpmethod:schema://host:port：协议、主机和端口

NOTE: 以 https://www.taobao.com/test 这个 url 并使用 GET 方法为例。对应的资源名有两种粒度，分别是
GET:[https://www.taobao.com](https://www.taobao.com/) 以及 GET:https://www.taobao.com/test。

##### Spring WebMVC Adapter

spring-cloud-starter-alibaba-sentinel默认已经集成了sentinel-spring-webmvc-adapter，通过SentinelWebInterceptor将HTTP请求变成Sentinel的Resource，提供限流规则配置。Sentinel从Controller中提取Web
Resource (e.g. `/foo/{id}`)
。如果不需要自动拦截Web，可以通过spring.cloud.sentinel.filter.enabled=false关闭。详情见[官网](https://github.com/alibaba/Sentinel/blob/master/sentinel-adapter/sentinel-spring-webmvc-adapter/README.md)

##### Spring Webflux Adapter

spring-cloud-starter-alibaba-sentinel默认已经集成了sentinel-spring-webflux-adapter，同上，这个是支持Webflux响应式应用。

##### 更多开源框架适配

请参考[官网](https://sentinelguard.io/zh-cn/docs/open-source-framework-integrations.html)

#### Spring Cloud Gateway 支持

参考 [Sentinel 网关限流](https://github.com/alibaba/Sentinel/wiki/网关限流)。

若想跟 Sentinel Starter 配合使用，需要加上 spring-cloud-alibaba-sentinel-gateway 依赖，同时需要添加
spring-cloud-starter-gateway 依赖来让 spring-cloud-alibaba-sentinel-gateway 模块里的 Spring Cloud Gateway 自动化配置类生效：

```xml

<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>
<dependency>
<groupId>com.alibaba.cloud</groupId>
<artifactId>spring-cloud-alibaba-sentinel-gateway</artifactId>
</dependency>
<dependency>
<groupId>org.springframework.cloud</groupId>
<artifactId>spring-cloud-starter-gateway</artifactId>
</dependency>
```

**注意**：

- Sentinel 网关流控默认的粒度是 route 维度以及自定义 API 分组维度，默认**不支持 URL 粒度**。若通过 Spring Cloud Alibaba
  接入，请将 `spring.cloud.sentinel.filter.enabled` 配置项置为 false（若在网关流控控制台上看到了 URL 资源，就是此配置项没有置为
  false）。
- 若使用 Spring Cloud Alibaba Sentinel 数据源模块，需要注意网关流控规则数据源类型是 `gw-flow`，若将网关流控规则数据源指定为
  flow 则不生效。

## 服务端介绍

Macula基于Sentinel-Dashboard扩展了对于nacos数据源的支持。请参考[官网](https://sentinelguard.io/zh-cn/docs/dashboard.html)

## 版权说明

- sentinel：https://github.com/alibaba/Sentinel/blob/master/LICENSE