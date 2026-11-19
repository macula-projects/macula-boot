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

配置文件加密支持 AES（AES/ECB/PKCS5Padding）。密钥通过启动参数 `macula.crypto.key`、JVM 系统属性或环境变量 `MACULA_CRYPTO_KEY` 提供，长度为 16、24 或 32 个 UTF-8 字节。示例密钥仅用于演示：

```
在启动的时候加入密钥
--macula.crypto.key=abcdef1234567890
```

***注意：配置文件的密文属性需要以 "mpw:" 开头***，例如：

```yaml
test:
  encrypt: mpw:Or0wNwIcctnekUyDfeN8cw==
```

以上示例读取 `test.encrypt` 得到 `hello1`。

#### Nacos 配置与刷新

本地配置和 Nacos 配置均使用相同的 `mpw:` 协议。应用需要引入 Nacos Config 依赖并完成原有的配置加载设置；Crypto Starter 不提供 Nacos 客户端或连接配置，也不增加 Spring Cloud 运行时依赖。

- 启动早期通过 `EnvironmentPostProcessor` 包装已加载的配置源，bootstrap 后通过 `ApplicationPreparedEvent` 补充包装后加载的 Nacos 配置，再进行普通 Bean 创建、`@Value` 注入与 `@ConfigurationProperties` 绑定。
- 按原名称、顺序包装普通 Map 和可枚举配置源，递归处理组合配置源；保留 `OriginTrackedMapPropertySource` 的具体类型，确保 Spring Cloud bootstrap 合并本地配置时仍可识别，避免丢失 `spring.application.name` 等属性。原数据继续保存密文，读取时解密，不建立最高优先级明文副本。高优先级明文配置仍能覆盖低优先级加密配置。
- Spring Cloud 发布以当前 `ConfigurableApplicationContext` 为 source 的标准 `EnvironmentChangeEvent` 时，监听器优先重新包装被替换的配置源。后续读取和配置重新绑定使用新值；配置改为明文、属性删除或整个配置源删除后，按原有优先级读取或回退，不保留旧明文。
- `@Value` 的普通单例不会因配置改变而自动重新注入；动态更新仍需要原有的 `@RefreshScope` 或 Spring Cloud 配置重新绑定机制。监听器依赖标准同步事件处理，不保证源替换与事件发布之间并发读取的原子性，不提供运行期间的密钥轮换机制。
- 密钥建议在进程启动时提供。系统环境变量保持其原类型和宽松绑定行为，用于提供密钥或普通覆盖值，不参与 `mpw:` 解密。缺少密钥时沿用既有行为，保留原配置值；提供密钥后，读取格式错误或无法解密的密文会抛出包含源名、属性名的异常，不输出密钥、密文或明文。

排查 Nacos 配置仍返回密文时，确认 Starter 在 classpath、启动密钥正确、配置值以 `mpw:` 开头；自定义刷新流程需在源替换后、重新绑定前发布标准事件。仅通过 `@NacosValue` 或其他客户端私有缓存读取、未进入 Spring Environment 的配置不在本处理器的处理范围。

验证采用离线组合配置源模拟 Nacos 加载、真实 Spring Boot 启动及 Spring Cloud 刷新事件/配置重新绑定，不等同于连接真实 Nacos 服务的端到端验证。

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

