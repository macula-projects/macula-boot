## 概述

本模块主要提供默认的日志格式配置、日志发送、日志审计等功能，由多个子模块组成。包括：

- macula-boot-starter-auditlog 日志审计记录
- macula-boot-starter-logstash 将日志发送给logstash

## 组件坐标

```xml
<!-- 日志审计 -->
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-auditlog</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用说明

### AuditLog

添加macula-boot-starter-auditlog依赖后，在需要审计的方法上加上@AuditLog注解，添加注解后调用方法会触发OperLogEvent事件，可以定义Listener监听事件用于持久化审计日志。

```java
  @Operation(summary = "新增菜单")
  @AuditLog(title = "新增菜单")
  @PostMapping
  @CacheEvict(cacheNames = "system", key = "'routes'")
  public boolean addMenu(@RequestBody SysMenu menu) {
      boolean result = menuService.saveMenu(menu);
      return result;
  }
```

@AuditLog注解定义如下：

```java
@Target({ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {
    /**
     * 模块
     */
    String title() default "";

    /**
     * 功能
     */
    BusinessType businessType() default BusinessType.OTHER;

    /**
     * 操作人类别
     */
    OperatorType operatorType() default OperatorType.MANAGE;

    /**
     * 是否保存请求的参数
     */
    boolean isSaveRequestData() default true;

    /**
     * 是否保存响应的参数
     */
    boolean isSaveResponseData() default true;

    /**
     * 排除指定的请求参数
     */
    String[] excludeParamNames() default {};

}
```

Listenter定义参考：

```java
@Component
@RequiredArgsConstructor
public class AuditLogEventListener {

    private final SysLogService sysLogService;

    /**
     * 保存系统日志记录
     */
    @Async
    @EventListener
    public void saveLog(OperLogEvent operLogEvent) {
        SysLog log = new SysLog();
        log.setOpIp(operLogEvent.getOperIp());
        log.setErrorMsg(operLogEvent.getErrorMsg());
        log.setJsonResult(operLogEvent.getJsonResult());
        log.setOpName(operLogEvent.getOperName());
        log.setOpParam(operLogEvent.getOperParam());
        log.setOpMethod(operLogEvent.getMethod());
        log.setOpStatus(operLogEvent.getStatus());
        log.setOpUrl(operLogEvent.getOperUrl());
        log.setOpRequestMethod(operLogEvent.getRequestMethod());
        log.setOpTitle(operLogEvent.getTitle());
        log.setCreateBy(operLogEvent.getOperName());
        if (operLogEvent.getOperTime() != null) {
            log.setCreateTime(LocalDateTime.ofInstant(operLogEvent.getOperTime().toInstant(), ZoneId.systemDefault()));
        } else {
            log.setCreateTime(LocalDateTime.now());
        }
        sysLogService.save(log);
    }
}
```

## 版权说明

上述部分代码来源于[RuoYi](https://plus-doc.dromara.org)
并适当修改。基于MIT协议：https://gitee.com/dromara/RuoYi-Cloud-Plus/blob/master/LICENSE