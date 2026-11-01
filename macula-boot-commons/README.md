# Macula Boot Commons

提供公共返回结构、结果码、异常、常量及上下文工具，无独立 YAML 配置项。Jackson API 使用 `tools.jackson.*`，共享注解仍为 `com.fasterxml.jackson.annotation.*`；空值注解使用 JSpecify。

## 统一的返回结构

`Result<T>` 包含 `success`、`code`、`msg`、`cause` 和 `data`。业务数据使用 `data`，失败原因使用 `cause`，不要混用。

```java
Result<String> ok = Result.success("完成");
Result<Void> failed = Result.failed(ApiResultCode.BIZ_ERROR, "业务失败原因");
Result<Void> status = Result.judge(true);
```

业务可实现 `ResultCode` 定义自己的错误码。Web Starter 负责 Controller 响应包装，Commons 本身不注册 Web 处理器。

## 全局常量

| 类型 | 用途 |
| --- | --- |
| `GlobalConstants` | 请求 ID、租户、Token 和灰度标签；默认租户 ID 为 `1` |
| `SecurityConstants` | 安全相关头、权限标识和默认白名单 |
| `CacheConstants` | 框架缓存键约定 |

跨线程传递 `TenantContextHolder`、`GrayVersionContextHolder` 时配合 [Async Starter](../macula-boot-starters/macula-boot-starter-async/README.md)；业务手动设置上下文后应及时清理。

## Hutool工具类

模块传递引入 `hutool-all`，可按需使用 `StrUtil`、`CollUtil`、`DateUtil` 等工具。涉及对外 JSON 契约时优先使用应用统一的 Jackson Mapper，避免不同工具的日期、数值及空值规则不一致。

## 全局异常类

| 类型 | 用途 |
| --- | --- |
| `MaculaException` | 框架运行时异常基类 |
| `BizException` | 带业务错误码和消息的业务异常 |
| `BizCheckException` | 需要业务确认或检查的异常 |

```java
throw new BizException(ApiResultCode.BIZ_ERROR, "订单状态不允许此操作");
```

异常到 HTTP 状态及响应体的映射见 [Web Starter](../macula-boot-starters/macula-boot-starter-web/README.md)；异常消息中不要包含密码、凭据或内部堆栈。


## 资源访问凭证公共契约

`dev.macula.boot.security.PasskeyResource` 声明 `getResourceId()`、`getPasskey()`、`setPasskey(String)`。实现类的列表可见性必须已经由业务完成当前用户的数据权限过滤；接口实现即表示允许框架为该记录签发访问凭证。资源编号须全局唯一；接口辅助 resourceId 在 JSON 中忽略，实际业务编号和 passkey 正常输出。

`PasskeyIssuer` 是 Web 响应处理可调用的签发扩展，由安全模块实现，无需 Commons 或 Web 依赖具体认证框架。`PasskeyResources` 仅从 Collection、PageVO、成功 Result 的明确包装中提取列表资源，拒绝循环/过深结构，不遍历任意业务属性，单对象不自动签发。

`ResourceAccessDenied` 是跨模块的资源拒绝标记，`ResourceAccessException` 提供无安全库依赖的拒绝异常；现有 Web 异常 Advice 将其转换为 HTTP 403 / A0312。签名密钥、当前身份与实际验签留在 Security Starter，不放入 DTO 或公共模块。
