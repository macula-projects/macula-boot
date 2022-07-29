# Web模块

## 配置

```yaml
macula:
  web:
    exception-advie: true   # 统一异常处理器，默认true
    response-advice: true   # 统一响应处理器，默认true
```

## 全局异常的处理

ControllerExceptionAdvice类默认会处理validation异常、ApiException异常，统一按照如下结构返回

```json
{
  "code": "10001",
  "msg": "参数异常",
  "data": "具体的异常信息" 
}
```

ApiException异常的message信息会通过data字段返回，支持国际化code。

## 全局返回结构处理

默认情况下所有Controller都按照Result结构体返回，如果不需要包装可以添加@NotControllerResponseAdvice注解

## 国际化

透过请求头的Accept-Language设定对应的国际化资源

## 时间格式

1. 当GET请求或者POST请求的x-www-form-urlencoded时，前端的时间格式都是通过Converter转为日期格式；
2. 如果是POST请求的application/json时，则由jackson的配置来完成
3. 响应如果是json格式也是由jackson的配置
4. 响应如果是text/html时，由生成html的模板引擎配置
5. 数据库和Java的实体建议使用无时区的字段，mysql的date对应LocalDate,
   time对应LocalTime，datetime对应LocalDateTime，timestamp对应ZonedDateTime（不建议用），jdbc url要配置serverTimeZone和数据库时区保持一致
6. [数据库与Java时间字段转换原理](https://www.jianshu.com/p/af8d7b3e2074)
7. 与外部交互的接口层的时间建议使用有时区的，比如Date、ZonedDateTime
   可以通过如下配置配置日期和时间格式

```yaml
spring:
  mvc:
    format:
      date: iso
      time: iso
      date-time: yyyy-MM-dd HH:mm:ss
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
```

如果需要支持跨时区，则需要设置为带时区的日期格式

```yaml
spring:
  mvc:
    format:
      date: iso
      time: iso
      date-time: iso-offset
  jackson:
    date-format: com.fasterxml.jackson.databind.util.StdDateFormat
```

**需要注意**

1. jackson.date-format如果不配置，默认就是ISO格式的，例如2022-05-02T10.12.000+08:00，Localxxx的时间类型是不可以携带时区的
2. mvc.format如果不配置，默认是根据系统语言的Localized(SHORT,SHORT)格式，建议一定要配置。配置了iso-offset时，LocalDateTime也需要携带时区，但是会被忽略

## Validator校验

默认引入了spring-boot-starter-validation，可以在这里继续扩展自定义的validator，比如身份证、电话、邮件等等

## 脱敏

通过使用@Sensitive注解可以令某个字段属性在序列化为JSON时按照一定的规则脱敏，并且预置了如下规则：

* MOBILE：手机号，如：138****2359
* CHINESE_NAME：中文名 ，如：黄**
* ID_CARD：身份证号，如：如511623********0537
* FIXED_PHONE：座机号，如：****1234
* ADDRESS：地址，如：广东省广州市天河区****
* EMAIL：电子邮件，如：g**@163.com
* BANK_CARD：银行卡，如：如622848*********5579
* CUSTOM_HIDE：自定义，有多少个字符替换成多少个*
* CUSTOM_RETAIN_HIDE：保留方式隐藏
* CUSTOM_OVERLAY：自定义,只替换成指定个*

例如：

```java

@Data
public class DemoDto {

   @NotEmpty
   private String password;

   @Length(min = 5, max = 25, message = "{key.length}")
   private String key;

   @Pattern(regexp = "[012]", message = "无效的状态标志")
   private String state;

   @Sensitive(value = Sensitive.Type.MOBILE)
   private String mobile;

}
```