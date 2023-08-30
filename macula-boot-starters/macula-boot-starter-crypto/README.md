## 概述

该模块提供数据加解密的能力。

## 组件坐标

```xml

<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-crypto</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

```yaml
macula:
  crypto:
    enabled: true   # 默认为true
    algorithm: AES  # BASE64, AES, RSA, SM2, SM4 
    encode: HEX     # BASE64, HEX
    password: xxx   # 对称加密的密钥
    publicKey: xxx  # 非对称加密公钥
    privateKey: xxx # 非对称解密私钥
```

## 核心功能

### 配置文件加密

配置文件加密只支持AES（AES/ECB/PKCS5Padding）方式加密，密钥只能通过命令行的方式设置

```
在启动的时候加入密钥
--macula.crypto.key=password
```

***注意：配置文件的密文属性需要以 "mpw:" 开头***，例如：

```yaml
test:
  encrypt: mpw:Or0wNwIcctnekUyDfeN8cw==
```

### MyBatis的数据库字段加密

#### 配合以下注解使用

```java
@Documented
@Inherited
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface CryptoField {

    /**
     * 加密算法
     */
    AlgorithmType algorithm() default AlgorithmType.DEFAULT;

    /**
     * 秘钥。AES、SM4需要
     */
    String password() default "";

    /**
     * 公钥。RSA、SM2需要
     */
    String publicKey() default "";

    /**
     * 公钥。RSA、SM2需要
     */
    String privateKey() default "";

    /**
     * 编码方式。对加密算法为BASE64的不起作用
     */
    EncodeType encode() default EncodeType.DEFAULT;

}
```

#### 举个栗子

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
  @Insert({"insert into user (`id`, `password`, ", "`user_name`, `age`)",
    "values (#{id,jdbcType=BIGINT}, #{password,jdbcType=VARCHAR}, ",
    "#{userName,jdbcType=VARCHAR}, #{age,jdbcType=INTEGER})"}) @Options(useGeneratedKeys = true)
  int insert(User record);
```

那么保存到数据库的数据中，userName 就是加密过的数据了。我们再来看一下查询

```java
@Select({"select", "`id`, `password`, `user_name`, `age`", "from user", "where `id` = #{id,jdbcType=BIGINT}"}) @Results(
    {@Result(column = "id", property = "id", jdbcType = JdbcType.BIGINT, id = true),
        @Result(column = "password", property = "password", jdbcType = JdbcType.VARCHAR),
        @Result(column = "user_name", property = "userName", jdbcType = JdbcType.VARCHAR),
        @Result(column = "age", property = "age", jdbcType = JdbcType.INTEGER)})
User selectByPrimaryKey(Long id);
```

返回的 user 对象中，userName 字段已经被自动解密

## 依赖引入

```xml

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>
    </dependency>

    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
    </dependency>

    <dependency>
        <groupId>org.bouncycastle</groupId>
        <artifactId>bcpkix-jdk18on</artifactId>
    </dependency>
</dependencies>
```

## 版权说明

- hutool：https://github.com/dromara/hutool/blob/v5-master/LICENSE
- bouncycastle：https://www.bouncycastle.org/licence.html