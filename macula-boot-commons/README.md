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

