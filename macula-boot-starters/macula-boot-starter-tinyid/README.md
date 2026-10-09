## 概述

通过 Feign 经 Macula Cloud 网关申请号段，在本地缓存、预加载并生成 ID，支持单个和批量取号。

## 组件坐标

```xml
<dependency>
    <groupId>dev.macula.boot</groupId>
    <artifactId>macula-boot-starter-tinyid</artifactId>
    <version>${macula.version}</version>
</dependency>
```

## 使用配置

与 system starter 共用网关配置：

```yaml
macula:
  cloud:
    endpoint: ${MACULA_CLOUD_ENDPOINT} # 网关基础地址，含 http(s)://
    app-key: ${MACULA_CLOUD_APP_KEY}
    secret-key: ${MACULA_CLOUD_SECRET_KEY}
```

服务端需预先配置 `bizType`，应用需具有号段接口访问权限。无需 TinyID 专用 token 或超时配置。

### 升级说明

- 移除旧 `macula.cloud.tinyid.server/token/connect-timeout/read-timeout`，改用统一网关配置。
- 网关将 `/tinyid/api/v1/id/nextSegmentIdSimple` 经 `StripPrefix=1` 转发；Server 不设 `/tinyid` 上下文，该接口使用 JWT 认证。
- 新号段接口使用网关应用权限，不再校验旧 Token 与 bizType 的绑定。客户端、Server 和路由需协调升级。

## 核心功能

### 单个与批量取号

注入 `IdGeneratorFactory` 后按业务类型取号：

```java
IdGenerator generator = idGeneratorFactory.getIdGenerator("order");
Long id = generator.nextId();
List<Long> ids = generator.nextId(10);
```

ID 趋势递增、不保证连续；网关或服务端短暂不可用时，仍可使用尚未耗尽的本地号段。
提供自定义 `SegmentIdService` 时，远程客户端自动退让，无需配置网关凭据；`IdGeneratorFactory` 也可自定义。

`TinyIdSysException` 继承 `BizException`，默认错误码为 `ApiResultCode.SYS_ERROR`，也支持
`new TinyIdSysException(resultCode, "异常详情", cause)`。未被业务捕获时可由现有 Web 异常处理器识别；
Server 号段接口成功仍返回五字段文本；失败返回 HTTP 500 和统一 Result JSON。
Feign 与本地生成器保留远程错误码及 cause，不再将远程失败当作空号段。

### MyBatis-Plus 主键生成器（可选）

提供 `dev.macula.boot.starter.tinyid.mybatisplus.TinyIdIdentifierGenerator`，实现 `IdentifierGenerator`，
适用于 `ASSIGN_ID`：

```java
new TinyIdIdentifierGenerator(idGeneratorFactory);
```

MyBatis-Plus 依赖为 `optional`，由使用方自行引入、注册 Bean 并配置使用，Starter 不自动注册。

生成器从实体映射中读取表名和主键列名，以 `表名_主键列名` 作为 `bizType`，例如 `sys_user_id`。
需预建对应业务；使用逻辑表名，映射缺失时明确报错。同名或拼接结果相同的映射共享序列，改名时需迁移发号进度。

## 依赖引入

主要依赖由 Starter 声明，版本由父 POM 统一管理：

| 依赖 | 用途 |
| --- | --- |
| `spring-boot-starter` | Spring Boot 基础支持 |
| `spring-boot-http-converter` | Feign 消息转换支持 |
| `macula-boot-starter-feign` | 网关远程调用与签名 |
| `mybatis-plus-core`（optional） | 可选主键生成器接口与实体元数据 |

## 版权说明

- [TinyID](https://github.com/didi/tinyid/blob/master/LICENSE)
