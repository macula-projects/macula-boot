# Macula Boot Commons

主要引入所有模块都需要使用到的工具类，基础类

## 统一的返回结构

```java
@Data
public class Result<T> implements Serializable {
    private boolean success;

    private String code;

    private String msg;

    private T data;

    public static <T> Result<T> success() {
        return success(null);
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setCode(ApiResultCode.SUCCESS.getCode());
        result.setMsg(ApiResultCode.SUCCESS.getMsg());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> failed() {
        return failed(ApiResultCode.FAILED);
    }

    public static <T> Result<T> failed(ResultCode resultCode) {
        return failed(resultCode, null);
    }

    public static <T> Result<T> failed(ResultCode resultCode, T data) {
        // data是错误原因
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(resultCode.getCode());
        result.setMsg(resultCode.getMsg());
        result.setData(data);
        return result;
    }

    public static <T> Result<T> judge(boolean status) {
        if (status) {
            return success();
        } else {
            return failed();
        }
    }
}
```

## 全局常量

## Hutool工具类

## 全局异常类

```java
// 框架异常基类
public class MaculaException extends RuntimeException {

    public MaculaException() {
        super();
    }

    public MaculaException(String message) {
        super(message);
    }

    public MaculaException(String message, Throwable cause) {
        super(message, cause);
    }

    public MaculaException(Throwable cause) {
        super(cause);
    }
}

// REST服务层异常
public class ApiException extends MaculaException {
    private ResultCode resultCode;

    public ApiException(ResultCode resultCode, String exceptionMessage) {
        // message用于用户设置抛出错误详情，例如：当前价格-5，小于0
        super(exceptionMessage);
        this.resultCode = resultCode;
    }

    public ApiException(String exceptionMessage) {
        super(exceptionMessage);
        this.resultCode = ApiResultCode.API_ERROR;
    }

    public ResultCode getResultCode() {
        return this.resultCode;
    }
}
```

## 资源访问凭证公共契约

`dev.macula.boot.security.PasskeyResource` 声明 `getResourceId()`、`getPasskey()`、`setPasskey(String)`。实现类的列表可见性必须已经由业务完成当前用户的数据权限过滤；接口实现即表示允许框架为该记录签发访问凭证。资源编号须全局唯一；接口辅助 `resourceId` 在 JSON 中忽略，实际业务编号和 `passkey` 正常输出。

`PasskeyIssuer` 是 Web 响应处理可调用的签发扩展，由安全模块实现，无需 Commons 或 Web 依赖具体认证框架。`PasskeyResources` 仅从 `Collection`、`PageVO`、成功 `Result` 的明确包装中提取列表资源，拒绝循环或过深结构，不遍历任意业务属性，单对象不自动签发。

`ResourceAccessDenied` 是跨模块的资源拒绝标记，`ResourceAccessException` 提供无安全库依赖的拒绝异常；现有 Web 异常 Advice 将其转换为 HTTP 403 / A0312。签名密钥、当前身份与实际验签留在 Security Starter，不放入 DTO 或公共模块。

