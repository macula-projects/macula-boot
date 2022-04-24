# API模块
对外输出的统一响应结构体，响应码可以集成ResultCode来定义，不同的业务定义不同的响应码

```java
public interface ResultCode {

	/**
	 * 返回码
	 *
	 * @return int
	 */
	int getCode();

	/**
	 * 返回消息
	 *
	 * @return String
	 */
	String getMsg();
}
```

返回结构体，正常返回可以使用Result.success方法，如果有返回数据可以使用Result.data方法，返回错误可以使用Result.fail方法：
```java
public class Result<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private int code;

    private String msg;

    private long time = System.currentTimeMillis();

    private T data;

    private Result(ResultCode resultCode) {
        this(resultCode, null, resultCode.getMsg());
    }

    private Result(ResultCode resultCode, String msg) {
        this(resultCode, null, msg);
    }

    private Result(ResultCode resultCode, T data, String msg) {
        this(resultCode.getCode(), data, msg);
    }

    private Result(int code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
        this.time = System.currentTimeMillis();
    }

    public static <T> Result<T> success() {
        return new Result<>(ApiResultCode.SUCCESS);
    }

    public static <T> Result<T> success(String msg) {
        return new Result<>(ApiResultCode.SUCCESS, msg);
    }

    public static <T> Result<T> data(T data) {
        return data(data, ApiConstant.DEFAULT_SUCCESS_MESSAGE);
    }

    public static <T> Result<T> data(T data, String msg) {
        return new Result<>(ApiResultCode.SUCCESS, data, msg);
    }

    public static <T> Result<T> fail() {
        return fail(ApiResultCode.FAILURE);
    }

    public static <T> Result<T> fail(ResultCode resultCode) {
        return new Result<>(resultCode);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(ApiResultCode.FAILURE, msg);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, null, msg);
    }

    public static <T> Result<T> condition(boolean flag) {
        return flag ? success(ApiConstant.DEFAULT_SUCCESS_MESSAGE) : fail(ApiConstant.DEFAULT_FAIL_MESSAGE);
    }
}
```

请求结构体，主要是分页和排序的传递：
```java
@Getter
@Setter
public class Search implements Serializable {

	/**
	 * 关键词
	 */
	private String keyword;

	/**
	 * 开始日期
	 */
	private String startDate;

	/**
	 * 结束日期
	 */
	private String endDate;

	/**
	 * 排序属性
	 */
	private String prop;

	/**
	 * 排序方式：asc,desc
	 */
	private String order;

	/**
	 * 当前页
	 */
	private Integer current = 1;

	/**
	 * 每页的数量
	 */
	private Integer size = 10;
}
```