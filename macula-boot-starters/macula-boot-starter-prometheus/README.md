## 概述

基于spring-boot-starter-actuator以及micrometer提供Prometheus监控所需端点。

## 组件坐标

```xml
<!-- 发送监控指标到prometheus -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-prometheus</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 核心功能

### 配置Health端点

如果只需要health端点，只需要引入spring-boot-starter-actuator即可。

### 配置Prometheus端点

引入macula-boot-starter-prometheus即可。其他在Prometheus控制台配置。

#### 配置端点

暴露health，metrics和prometheus端点。默认不配置只会暴露health

```yaml
# 监控配置
management:
  endpoint:
    metrics:
      enabled: true
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

#### 定义自定义指标

接下来，我们需要定义自定义指标。创建一个名为CustomMetrics的Java类，并添加以下代码：

```java
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CustomMetrics {
    private final Counter customCounter;
    public CustomMetrics(MeterRegistry registry) {
        customCounter = registry.counter("custom_metric_counter");
    }

    public void incrementCustomCounter() {
        customCounter.increment();
    }
}
```

以上代码创建了一个名为custom_metric_counter的计数器指标。

#### 在代码中更新指标值

```java
@RestController
public class MyController {
    private final CustomMetrics customMetrics;

    public MyController(CustomMetrics customMetrics) {
        this.customMetrics = customMetrics;
    }

    @GetMapping("/my-endpoint")
    public String myEndpoint() {
        // 业务逻辑代码

        // 增加自定义计数器的值
        customMetrics.incrementCustomCounter();

        return "Success";
    }
}
```

在上面的示例中，我们在myEndpoint方法中使用CustomMetrics类的incrementCustomCounter方法来增加计数器的值。

#### 验证指标是否成功写入Prometheus

最后，我们可以通过访问http://localhost:8080/actuator/prometheus来验证指标是否成功写入Prometheus。在返回的结果中，你应该能够看到类似以下内容的指标：

```
#HELP custom_metric_counter
#TYPE custom_metric_counter counter
custom_metric_counter 1.0
```

## 依赖引入

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
</dependencies>
```

## 版本说明

spring-boot：https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt

micrometer：https://github.com/micrometer-metrics/micrometer/blob/main/LICENSE