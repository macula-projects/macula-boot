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
  type-enums-package: org.macula.boot.starter.mproot.test.entity.enums
```
要使用通用枚举，需要定义枚举类型，同时记得上述配置加入到你的应用配置中去
[官方文档](https://baomidou.com/pages/8390a4/)

### 企业级的支持
可以使用mybatis-mate企业版，来支持字段加解密、数据脱敏、
添加mybatis-mate依赖
```xml
<dependencies>
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-mate-starter</artifactId>
    </dependency>
    
    <dependency>
        <groupId>javax.servlet</groupId>
        <artifactId>javax.servlet-api</artifactId>
    </dependency>
</dependencies>
```
添加配置项
```yaml
mybatis-mate:
  cert:
    # 请添加微信wx153666购买授权，不白嫖从我做起！ 测试证书会失效，请勿正式环境使用
    grant: thisIsTestLicense
    license: as/bsBaSVrsA9FfjC/N77ruEt2/QZDrW+MHETNuEuZBra5mlaXZU+DE1ZvF8UjzlLCpH3TFVH3WPV+Ya7Ugiz1Rx4wSh/FK6Ug9lhos7rnsNaRB/+mR30aXqtlLt4dAmLAOCT56r9mikW+t1DDJY8TVhERWMjEipbqGO9oe1fqYCegCEX8tVCpToKr5J1g1V86mNsNnEGXujnLlEw9jBTrGxAyQroD7Ns1Dhwz1K4Y188mvmRQp9t7OYrpgsC7N9CXq1s1c2GtvfItHArkqHE4oDrhaPjpbMjFWLI5/XqZDtW3D+AVcH7pTcYZn6vzFfDZEmfDFV5fQlT3Rc+GENEg==
  # 开启数据审计
  audit: true
  encryptor:
    # 对称算法密钥，随机字符串作为密钥即可（有些算法长度有要求，注意）
    password: qmh9MK4KsZY8FnqkJYk8tzgc0H
    # 非对称加密 RSA 公钥私钥
    publicKey: MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCEOCMScPeNaJ0DP9N9vd/fXwPGUVnuxeGPpRePXfWuX/X/Yk5IMhwEfYLXictxQk/oAqGnqtDuS/PCL/7mqL+8wFSYnWWErCSkDdT6LjyD07l9dWv+Xj1UTEjP24sEgYA92f4AZyvhsw8I/Bj6a9a30r+kVOGoEZgGMf2c2xK4CQIDAQAB
    privateKey: MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAIQ4IxJw941onQM/0329399fA8ZRWe7F4Y+lF49d9a5f9f9iTkgyHAR9gteJy3FCT+gCoaeq0O5L88Iv/uaov7zAVJidZYSsJKQN1PouPIPTuX11a/5ePVRMSM/biwSBgD3Z/gBnK+GzDwj8GPpr1rfSv6RU4agRmAYx/ZzbErgJAgMBAAECgYBAlFK9DSQ8k14tWh1oizcvmO71DIMKluhHCvHo+pGnLAOxS0jFBoScxNkFga42kZcJ0U8337zQx5Q1ws+TxdRwHxQO889ZGQH3kOFB0ErUMTgFrTOakZhV0dMWzebkYitNcduSKZ1yfgM5ekF9k3owPIQhUNy8eXjagiLLb9/woQJBALwofOx+fuanQLC1yotFqYAx0XED9EpVPhS/G8mc4dZSNWZ548bIyq3ozP0CoHqriQo/X3NVzIJOU3rhn92fwj0CQQCz5FaeHzSqe1H4bTxzwgR5BUHttxrAPtktwfgCRgaSrZByjFldtP/dGaJmjR2Vzp848WcusJZlSlaLTfndm6W9AkEAoSxlZgctGNKn3Ta7mvU/Lmp+J7rlZU8DcK4LVXYnFXkx+OfsLvkMdE/4V7oKUUnih36lepxCJFSHubjPQf55WQJBAIUa8yxUkreCQAi9avmMGZsiVMH7tgOBfVjqKQQlpD9rxXG8f3Nitd93VD7lM3rhQ9byaBKX/vA7rQWuUK+0t1ECQDTGhLRJFZK4J7UGklTX94pknM/5rO3N/JPkFJcGilbgzkqy0s13D1K+8cR0qTn2DPW8vPoLQpVGuaATTTmMdvg=

```
详情见官方文档
[官方文档](https://baomidou.com/pages/1864e1/)

## 复杂查询
默认情况下自定义SQL的XML配置文件放在src/main/resources/mapper/*.xml目录下，不要放在src/main/java目录下，有可能编译的时候会忽略xml等资
源文件。如果不是放在/mapper目录下，需要单独配置：
```yaml
mybatis-plus:
  mapper-locations: classpath:/org/macula/**/mapper/*Mapper.xml
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
