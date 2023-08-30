## 概述

该模块为web开发所需的基本依赖包。是对spring-boot-starter-web模块的扩展，并且使用undertow替换了tomcat作为WEB服务器。有兴趣可以阅读[优雅的写Controller](https://mp.weixin.qq.com/s/i1iCiwhTxQRMqIQj6QzbiQ)。

## 组件坐标

```xml

<dependency>
   <groupId>dev.macula.boot</groupId>
   <artifactId>macula-boot-starter-web</artifactId>
   <version>${macula.version}</version>
</dependency> 
```

## 使用配置

```yaml
macula:
  web:
    exception-advie: true   # 统一异常处理器，默认true
    response-advice: true   # 统一响应处理器，默认true
```

## 核心功能

### 全局异常处理

如果在Controller每个方法都catch异常，非常不方便，所以默认通过ControllerExceptionAdvice类处理，包括：

- BindException（使用form data方式调用接口，校验异常抛出 BindException）
- ConstraintViolationException（单个参数校验异常抛出ConstraintViolationException）
- MethodArgumentNotValidException (使用 json 请求体调用接口，校验异常抛出 MethodArgumentNotValidException)
- BizException（业务异常，一般业务可以继承这个异常类）
- NullPointerException
- Exception（兜底）

统一按照如下结构返回，HTTP Status返回是500：

```json
{
  "code": "10001",
  "msg": "异常说明",
  "data": "具体的异常信息" 
}
```

BizException异常的message信息会通过data字段返回，支持国际化code。其定义如下：

```java
public class BizException extends MaculaException {

   private String code;
   private String msg;

   public BizException(ResultCode resultCode, String exceptionMessage) {
      this(resultCode.getCode(), resultCode.getMsg(), exceptionMessage);
   }

   public BizException(String code, String msg, String exceptionMessage) {
      // message用于用户设置抛出错误详情，例如：当前价格-5，小于0
      super(exceptionMessage);
      this.code = code;
      this.msg = msg;
   }

   public BizException(String exceptionMessage) {
      this(ApiResultCode.BIZ_ERROR, exceptionMessage);
   }

   public String getCode() {
      return this.code;
   }

   public String getMsg() {
      return this.msg;
   }
}
```

> 大家在开发应用时，在service层可以讲底层异常使用BizException封装为业务异常抛出。在Controller层一般情况下无需catch任何异常，交由上述全局异常处理器统一处理。

### 全局返回结构处理

默认情况下所有Controller都按照Result结构体返回，Controller返回值无需自己转换为Result类，保持原始返回类即可。

***如果不需要包装可以添加@NotControllerResponseAdvice注解。***

Result类由macula-boot-commons模块定义。具体如下：

```java

@Data
public class Result<T> implements Serializable {
   private boolean success;
   private String code;
   private String msg;
   private T data;
}
```

并且为了code属性的规范一致，提供了ResultCode接口

```java
public interface ResultCode {
   /**
    * 返回码
    */
   String getCode();

   /**
    * 返回消息
    */
   String getMsg();
}
```

macula默认提供了ApiResultCode实现，定义了用户类异常、业务类异常以及第三方调用异常。

建一个各项目自行定义自己的ResultCode，code的编码规则建议如下：

- 长度为5位
- 第一位字母，表示一类异常，macula保留A用户类异常、B业务类异常、C第三方调用异常等三个字母。
- 后四位用数字表示，同类型的异常尽量保持前两位一致

#### Controller示例

```java

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/consumer")
public class ConsumerController {

   @Autowired
   private Provider1Service provider1Service;

   @Autowired
   private GapiService gapiService;

   @Autowired
   private IpaasService ipaasService;

   @GetMapping("/echo")
   @NotControllerResponseAdvice
   // 该接口直接返回String
   public String echo() {
      return provider1Service.echo("consumer");
   }

   @PostMapping("/user")
   // 该接口返回的JSON是{sucess:true, code:xxx, msg: xxx, data: {UserResult}}
   public UserResult getUser() {
      return provider1Service.getUser();
   }
}
```

### 国际化

客户端透过请求头的Accept-Language设定对应的国际化资源，其他参考Spring标准。

### 时间格式

#### 日期传入

http请求传入日期格式的字符串

##### 表单请求

当GET请求或者POST请求的x-www-form-urlencoded时，前端的时间格式都是通过Converter转为日期格式，系统默认已经设置为ISO日期格式。

```java
@Override public void addFormatters(FormatterRegistry registry){
        // 默认String没有办法转为java.util.Date类型
        registry.addConverter(new Converter<String, Date>(){
@Override public Date convert(String source){
        return DateUtil.parse(source.trim()).toJdkDate();
        }
        });

        DateTimeFormatterRegistrar registrar=new DateTimeFormatterRegistrar();
        registrar.setUseIsoFormat(true);
        registrar.registerFormatters(registry);
        }

enum ISO {
   /**
    * The most common ISO Date Format {@code yyyy-MM-dd} &mdash; for example,
    * "2000-10-31".
    */
   DATE,

   /**
    * The most common ISO Time Format {@code HH:mm:ss.SSSXXX} &mdash; for example,
    * "01:30:00.000-05:00".
    */
   TIME,

   /**
    * The most common ISO Date Time Format {@code yyyy-MM-dd'T'HH:mm:ss.SSSXXX}
    * &mdash; for example, "2000-10-31T01:30:00.000-05:00".
    */
   DATE_TIME
}
```

{{% alert title="提示" color="primary" %}}

如果请求的日期字符串对应的类型是java.util.Date类型，我们使用了hutool的DateUtil.parse，他会自适应一些日期格式，具体参考[hutool](https://doc.hutool.cn/pages/DateUtil/#%E5%AD%97%E7%AC%A6%E4%B8%B2%E8%BD%AC%E6%97%A5%E6%9C%9F)。

如果请求的日期字符串对应的类型是jsr310的，需要注意按照ISO要求。

如果某些字段或者参数需要个性化设置，可以使用注解@DateTimeFormat

{{% /alert %}}

##### JSON请求

如果是POST请求的application/json时，则由jackson的配置来完成

```yaml
spring:
  jackson:
     date-format: yyyy-MM-dd HH:mm:ss        # 该配置只会影响java.util.Date类型
```

{{% alert title="提示" color="primary" %}}

需要注意的是 spring.jackson.date-format这种全局化配置对于java8中localDate和LocalDateTime是无效的，对于localDate和LocalDateTime可以在类属性上设置，如下：

{{% /alert %}}

```java
@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") private LocalDateTime createTime;
```

#### 日期返回

##### 普通响应

响应如果是text/html时，由生成html的模板引擎配置

##### JSON响应

响应如果是json格式也是由jackson的配置

#### 数据库日期

数据库和Java的实体建议使用无时区的字段，mysql的date对应LocalDate,time对应LocalTime，datetime对应LocalDateTime，timestamp对应ZonedDateTime（不建议用），jdbc
url要配置serverTimeZone和数据库时区保持一致

- [数据库与Java时间字段转换原理](https://www.jianshu.com/p/af8d7b3e2074)

- 与外部交互的接口层的时间建议使用有时区的，比如Date、ZonedDateTime

{{% alert title="提示" color="primary" %}}

ZonedDateTime在转为LocalDateTime的时候切记要先设置为当前时区：

{{% /alert %}}

```java
// 转为LocalDateTime需要先把ZondeDateTime转为需要的时区，直接toLocalDateTime是没有换算时区的
LocalDateTime ldt=zdt.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
```

### Validator校验

默认引入了spring-boot-starter-validation，可以在这里继续扩展自定义的validator，比如身份证、电话、邮件等等。

```java
public class UserVO {
   @NotNull(message = "age 不能为空")
   private Integer age;
}
```

然后在`controller`方法中添加`@Validated`：

```java
// 注意这里的BindingResult参数可以不添加，如果不添加则validator有问题会抛出异常，由全局异常拦截器统一处理
public String add1(@Validated UserVO userVO,BindingResult result){
        List<FieldError> fieldErrors=result.getFieldErrors();
        if(!fieldErrors.isEmpty()){
        return fieldErrors.get(0).getDefaultMessage();
        }
        return"OK";
        }
```

内置的校验注解有很多，罗列如下：

| 注解               | 校验功能                |
|------------------|---------------------|
| @AssertFalse     | 必须是false            |
| @AssertTrue      | 必须是true             |
| @DecimalMax      | 小于等于给定的值            |
| @DecimalMin      | 大于等于给定的值            |
| @Digits          | 可设定最大整数位数和最大小数位数    |
| @Email           | 校验是否符合Email格式       |
| @Future          | 必须是将来的时间            |
| @FutureOrPresent | 当前或将来时间             |
| @Max             | 最大值                 |
| @Min             | 最小值                 |
| @Negative        | 负数（不包括0）            |
| @NegativeOrZero  | 负数或0                |
| @NotBlank        | 不为null并且包含至少一个非空白字符 |
| @NotEmpty        | 不为null并且不为空         |
| @NotNull         | 不为null              |
| @Null            | 为null               |
| @Past            | 必须是过去的时间            |
| @PastOrPresent   | 必须是过去的时间，包含现在       |
| @Pattern         | 必须满足正则表达式           |
| @PositiveOrZero  | 正数或0                |
| @Size            | 校验容器的元素个数           |

#### 分组校验

如果同一个参数，需要在不同场景下应用不同的校验规则，就需要用到分组校验了。比如：新注册用户还没起名字，我们允许`name`
字段为空，但是不允许将名字更新为空字符。

分组校验有三个步骤：

1. 定义一个分组类（或接口）
2. 在校验注解上添加`groups`属性指定分组
3. `Controller`方法的`@Validated`注解添加分组类

```java
public interface Update extends Default{
}
public class UserVO {
    @NotBlank(message = "name 不能为空",groups = Update.class)
    private String name;
    // 省略其他代码...
}
@PostMapping("update")
public ResultInfo update(@Validated({Update.class}) UserVO userVO) {
    return new ResultInfo().success(userVO);
}
```

细心的同学可能已经注意到，自定义的`Update`分组接口继承了`Default`接口。校验注解(如：` @NotBlank`)和`@validated`
默认都属于`Default.class`分组，这一点在`javax.validation.groups.Default`注释中有说明

```java
/**
 * Default Jakarta Bean Validation group.
 * <p>
 * Unless a list of groups is explicitly defined:
 * <ul>
 *     <li>constraints belong to the {@code Default} group</li>
 *     <li>validation applies to the {@code Default} group</li>
 * </ul>
 * Most structural constraints should belong to the default group.
 *
 * @author Emmanuel Bernard
 */
public interface Default {
}
```

在编写`Update`分组接口时，如果继承了`Default`，下面两个写法就是等效的：

```
@Validated({Update.class})
@Validated({Update.class,Default.class})
```

请求一下`/update`接口可以看到，不仅校验了`name`字段，也校验了其他默认属于`Default.class`分组的字段

```json
{
    "status": 400,
    "message": "客户端请求参数错误",
    "response": [
        "name 不能为空",
        "age 不能为空",
        "email 不能为空"
    ]
}
```

如果`Update`不继承`Default`，`@Validated({Update.class})`就只会校验属于`Update.class`分组的参数字段，修改后再次请求该接口得到如下结果，可以看到，
其他字段没有参与校验：

```json
{
    "status": 400,
    "message": "客户端请求参数错误",
    "response": [
        "name 不能为空"
    ]
}
```

#### 递归校验

如果 UserVO 类中增加一个 OrderVO 类的属性，而 OrderVO 中的属性也需要校验，就用到递归校验了，只要在相应属性上增加`@Valid`
注解即可实现（对于集合同样适用）

OrderVO类如下

```java
public class OrderVO {
   @NotNull
   private Long id;
   @NotBlank(message = "itemName 不能为空")
   private String itemName;
   // 省略其他代码...
}
```

在 UserVO 类中增加一个 OrderVO 类型的属性

```java
public class UserVO {
   @NotBlank(message = "name 不能为空", groups = Update.class)
   private String name;
   //需要递归校验的OrderVO
   @Valid
   private OrderVO orderVO;
   // 省略其他代码...
}   
```

#### 自定义校验

Spring 的 validation 为我们提供了这么多特性，几乎可以满足日常开发中绝大多数参数校验场景了。但是，一个好的框架一定是方便扩展的。有了扩展能力，就能应对更多复杂的业务场景，毕竟在开发过程中，
**唯一不变的就是变化本身**。

Spring Validation允许用户自定义校验，实现很简单，分两步：

1. 自定义校验注解
2. 编写校验者类

代码也很简单，结合注释你一看就能懂

```java
@Target({METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {HaveNoBlankValidator.class})// 标明由哪个类执行校验逻辑
public @interface HaveNoBlank {
    
    // 校验出错时默认返回的消息
    String message() default "字符串中不能含有空格";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

    /**
     * 同一个元素上指定多个该注解时使用
     */
    @Target({ METHOD, FIELD, ANNOTATION_TYPE, CONSTRUCTOR, PARAMETER, TYPE_USE })
    @Retention(RUNTIME)
    @Documented
    public @interface List {
        NotBlank[] value();
    }
}
public class HaveNoBlankValidator implements ConstraintValidator<HaveNoBlank, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // null 不做检验
        if (value == null) {
            return true;
        }
        if (value.contains(" ")) {
            // 校验失败
            return false;
        }
        // 校验成功
        return true;
    }
}
```

自定义校验注解使用起来和内置注解无异，在需要的字段上添加相应注解即可，同学们可以自行验证

### 字段脱敏

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

@Sensitive注解定义：

```java
/**
 * {@code Sensitvie} 脱敏的JSON注解
 *
 * @author rain
 * @since 2022/7/28 23:48
 */
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveSerializer.class)
public @interface Sensitive {

   /**
    * 脱敏的类型,默认手机号
    *
    * @return 返回脱敏类型
    */
   Type value();

   /**
    * CUSTOM_HIDE/CUSTOM_OVERLAY 时生效
    *
    * @return 开始位置（包含）
    */
   int startInclude() default 0;

   /**
    * CUSTOM_HIDE/CUSTOM_OVERLAY 时生效
    *
    * @return 结束位置（不包含）
    */
   int endExclude() default 0;

   /**
    * CUSTOM_OVERLAY 时生效
    *
    * @return *重复的次数
    */
   int overlayRepeat() default 4;

		...
}
```

### Jackson配置

默认对Jackson做了如下配置

```java
@Bean @ConditionalOnMissingBean public Jackson2ObjectMapperBuilderCustomizer customizer(){
        return builder->{
        builder.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        builder.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        builder.serializerByType(Long.class,ToStringSerializer.instance);
        builder.modulesToInstall(new JavaTimeModule());
        };
        }
```

### 租户ID

在HTTP请求头或者请求参数中添加`tenantId`参数可以设置当前请求的租户ID上下文，用户后续的数据库操作按照租户ID隔离数据。一般用在前端界面切换不同租户来管理。

## 依赖引入

```xml

<dependencies>
   <dependency>
      <groupId>dev.macula.boot</groupId>
      <artifactId>macula-boot-commons</artifactId>
   </dependency>
   <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
      <exclusions>
         <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
         </exclusion>
      </exclusions>
   </dependency>
   <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-undertow</artifactId>
   </dependency>
   <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-validation</artifactId>
   </dependency>
</dependencies>
```

## 版权说明

- Spring：https://github.com/spring-projects/spring-boot/blob/main/LICENSE.txt