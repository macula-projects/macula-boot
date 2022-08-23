# MyBatis Plus 模块

## 引入Mybatis Plus

该模块主要使用alibaba druid作为数据源工具，mybatis-plus作为数据库操作框架

## 配置
默认自动加载MyBatisPlusAutoConfiguration，已经开启了EnableTransactionManagement事务注解
可配置属性：
```yaml
macula:
  mybatis-plus:
    audit:
      create-by-name: createBy                #创建人
      create-time-name: createTime            #创建时间
      last-update-by-name: lastUpdateBy       #最后更新人
      last-update-time-name: lastUpdateTime   #最后更新时间
```
## 核心功能
### druid数据源配置
数据源的配置请参考[druid-spring-boot-starter 官方文档](https://github.com/alibaba/druid/tree/master/druid-spring-boot-starter)

### BaseEntity
默认提供了ID和审计字段，ID默认是自增长模式，如果数据库的默认字段不是标准的，请自行编写基础的Entity。同时记得在配置文件中设置审计字段的名称

### 主键策略
@TableId注解用于设置主键字段，通过IdType设置采用的主键策略

#### 自动增长
IdType.AUTO：使用数据库自增长ID

#### 数据库序列
IdType.INPUT： 如果要使用数据库的序列，要使用@KeySequence注解，同时要定义IKeyGenerator实现类并注册到Spring

#### 分配ID
IdType.ASSIGN_ID和ASSIGN_UUID：默认使用系统提供的雪花算法，如要自定义雪花算法，则需要实现IdentifierGenerator接口，并且注册到Spring，
但是不能与IKeyGenerator同时注册。
[官方文档](https://baomidou.com/pages/223848/#tableid)

## 扩展功能
### 逻辑删除
通过@TableLogic注解标识逻辑删除字段
[官方文档](https://baomidou.com/pages/6b03c5/#%E4%BD%BF%E7%94%A8%E6%96%B9%E6%B3%95)

### 通用枚举

```yaml
mybatis-plus:
  type-enums-package: dev.macula.i18n-base.starter.mproot.test.entity.enums
```

要使用通用枚举，需要定义枚举类型，同时记得上述配置加入到你的应用配置中去
[官方文档](https://baomidou.com/pages/8390a4/)

### 字段加密保存

配置如下：

```yaml
macula:
  mybatis-plus:
    aes:
      mode: ECB                       # 默认ECB
      padding: PKCS5Padding           # 默认PKCS5Padding
      key: 0CoJUm6Qyw8W8jud           # 请自行设置
      iv:                             # CBC等模式需要IV参数，其他可以不设置
```

字段加解密有两种方式

#### 使用@CryptoField注解

简单的说，只要你的 java bean 属性上加了这个注解

**以这个 java bean 作为入参，会对该参数的数据自动加密**

**以这个 java bean 作为出参（也可以是 List 中包含），会对该参数的数据自动解密**

##### 举个栗子

定义一个java bean, 在 userName 上添加该注解

```java
public class User implements Serializable {

  private Long id;
  private String password;
  @CryptoField
  private String userName;
  private Integer age;
 // 省略 getter setter
  
}
```

添加一个 insert 方法

```java
  @Insert({
      "insert into user (`id`, `password`, ",
      "`user_name`, `age`)",
      "values (#{id,jdbcType=BIGINT}, #{password,jdbcType=VARCHAR}, ",
      "#{userName,jdbcType=VARCHAR}, #{age,jdbcType=INTEGER})"
  })
  @Options(useGeneratedKeys = true)
  int insert(User record);
```

那么保存到数据库的数据中，userName 就是加密过的数据了

我们再来看一下查询

```java
  @Select({
    "select",
    "`id`, `password`, `user_name`, `age`",
    "from user",
    "where `id` = #{id,jdbcType=BIGINT}"
})
@Results({
    @Result(column="id", property="id", jdbcType=JdbcType.BIGINT, id=true),
    @Result(column="password", property="password", jdbcType=JdbcType.VARCHAR),
    @Result(column="user_name", property="userName", jdbcType=JdbcType.VARCHAR),
    @Result(column="age", property="age", jdbcType=JdbcType.INTEGER)
})
  User selectByPrimaryKey(Long id);
```

返回的 user 对象中，userName 字段已经被自动解密

## 复杂查询

默认情况下自定义SQL的XML配置文件放在src/main/resources/mapper/*.xml目录下，不要放在src/main/java目录下，有可能编译的时候会忽略xml等资
源文件。如果不是放在/mapper目录下，需要单独配置：

```yaml
mybatis-plus:
  mapper-locations: classpath:/dev/macula/**/mapper/*Mapper.xml
```

## 默认开启插件
### 分页插件
引入PaginationInnerInterceptor分页拦截器，返回IPage<T>接口
[官方文档](https://baomidou.com/pages/97710a)

### 乐观锁插件
引入OptimisticLockerInnerInterceptor乐观锁插件，更新具有@Version注解的实体时会自动加上version=xx条件
这种情况下需要判断update的返回值是否是>=1或者是否是true，以判断更新是否成功。
[官方文档](https://baomidou.com/pages/0d93c0)

### 防全表更新与删除插件
[官方文档](https://baomidou.com/pages/f9a237)

### 自动填充审计字段
默认的审计字段名称是createTime, lastUpdateTime, createBy, lastUpdateBy。数据库中的字段请转换为下划线。该名称有配置属性决定。
如果你先于MyBatisPlusAutoConfiguration定义MetaObjectHandler，则会使用你定义的类
