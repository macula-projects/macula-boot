# Macula Boot Starter Crypto

### 加解密模块

```yaml
macula:
  crypto:
    enabled: true   # 默认为true
    algorithm: AES  # BASE64, AES, RSA, SM2, SM4 
    password: xxx   # 对称加密的密钥
    publicKey: xxx  # 非对称加密公钥
    privateKey: xxx # 非对称解密私钥
    encode: HEX     # BASE64, HEX
```

### 对于mybatis的数据库字段加密，配合下述注解使用

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

### 对于配置文件

配置文件加密只支持AES（AES/ECB/PKCS5Padding）方式加密，密钥只能通过命令行的方式设置

```
在启动的时候加入密钥
--macula.crypto.key=password
```

配置文件的密文属性需要以 "mpw:" 开头