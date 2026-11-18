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
    enable: true    # 显式启用；未配置时不创建 CryptoManager
    algorithm: AES  # BASE64, AES, RSA, SM2, SM4
    encode: HEX     # BASE64, HEX
    password: xxx   # 对称加密的密钥
    publicKey: xxx  # 非对称加密公钥
    privateKey: xxx # 非对称解密私钥
```

配置前缀为 `macula.crypto`，密钥应由部署环境注入，不要提交到仓库。

| 属性 | 默认值 | 说明 |
| --- | --- | --- |
| `enable` | 未设置 | 必须为 `true` 才启用字段加解密自动配置 |
| `algorithm` | 未设置 | 默认算法：`BASE64`、`AES`、`RSA`、`SM2`、`SM4` |
| `encode` | 未设置 | 密文编码：`BASE64` 或 `HEX` |
| `password` | 未设置 | 对称加密密钥 |
| `public-key` / `private-key` | 未设置 | 非对称加密公钥 / 私钥 |
| `key` | 未设置 | `mpw:` 配置解密密钥，与字段加密的 `password` 不同 |

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


### 独立配置加解密工具

在仓库根目录使用 JDK 17 或更高版本构建：

```bash
mvn -pl macula-boot-starters/macula-boot-starter-crypto -am package -DskipTests
```

产物位于模块 `target/` 下：

- `macula-boot-starter-crypto-6.1.0-SNAPSHOT.jar`：普通 Starter。
- `macula-boot-starter-crypto-6.1.0-SNAPSHOT-cli.jar`：包含工具及所需 Hutool 依赖的独立可执行 JAR，可单独复制运行，无需 Spring、Nacos 或网络。

以下命令在模块目录执行；版本变更后相应调整文件名。示例密钥仅供演示：

```bash
# 加密，stdout 输出完整 mpw: 密文
java -jar target/macula-boot-starter-crypto-6.1.0-SNAPSHOT-cli.jar encrypt --key 'abcdef1234567890' --value 'hello1'
# 结果：mpw:Or0wNwIcctnekUyDfeN8cw==

# 解密，stdout 输出明文
java -jar target/macula-boot-starter-crypto-6.1.0-SNAPSHOT-cli.jar decrypt --key 'abcdef1234567890' --value 'mpw:Or0wNwIcctnekUyDfeN8cw=='

# 交互选择加密/解密，再输入密钥和内容
java -jar target/macula-boot-starter-crypto-6.1.0-SNAPSHOT-cli.jar

# 也可仅省略密钥或内容，缺失项通过交互补齐
java -jar target/macula-boot-starter-crypto-6.1.0-SNAPSHOT-cli.jar encrypt

# 帮助
java -jar target/macula-boot-starter-crypto-6.1.0-SNAPSHOT-cli.jar --help
```

密钥必须与应用启动时传入的 `macula.crypto.key` 相同，为 16、24 或 32 个 UTF-8 字节，不能全为空白。合法密钥和内容均不会自动去除首尾空白。空字符串明文通过 `--value ''` 输入；内容包含换行时使用带引号的参数，交互每次读取一整行。以短横线开头的普通值可直接传入；值恰好为 `--key`、`--value`、`--help` 时，请省略该选项并通过交互输入。

命令行参数可能出现在历史或进程信息中。有 Console 的交互终端会隐藏密钥及待加密明文；IDE 或管道无 Console 时从 UTF-8 stdin 读取，不保证终端不回显。提示和错误写 stderr，参数模式成功时 stdout 只有结果及换行；退出码 0 表示成功或帮助，2 表示参数/格式错误，1 表示输入或加解密失败。

也可直接调用工具类：

```java
String encrypted = ConfigCryptoUtil.encrypt("hello1", "abcdef1234567890");
String plaintext = ConfigCryptoUtil.decrypt(encrypted, "abcdef1234567890");
// import dev.macula.boot.starter.crypto.utils.ConfigCryptoUtil;
```

工具沿用配置协议 AES/ECB/PKCS5Padding 和 Base64；该协议无认证，无法保证识别所有错误密钥或密文篡改。工具不会改变 `CryptoPostProcessor` 的配置来源或动态刷新支持范围。

