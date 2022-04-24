package org.macula.boot.starter.api;

import lombok.Data;
import lombok.Getter;
import org.macula.boot.starter.api.constant.ApiConstant;

import java.io.Serializable;

/**
 * 统一响应消息报文
 *
 * @param <T> 　T对象
 * @author pangu
 */
@Data
public class Result<T> implements Serializable {


	private String code;

	private T data;

	private String msg;

	private long total;

	public static <T> Result<T> success() {
		return success(null);
	}

	public static <T> Result<T> success(T data) {
		ApiResultCode rce = ApiResultCode.SUCCESS;
		if (data instanceof Boolean && Boolean.FALSE.equals(data)) {
			rce = ApiResultCode.SYSTEM_EXECUTION_ERROR;
		}
		return result(rce, data);
	}

	public static <T> Result<T> success(T data, Long total) {
		Result<T> result = new Result<>();
		result.setCode(ApiResultCode.SUCCESS.getCode());
		result.setMsg(ApiResultCode.SUCCESS.getMsg());
		result.setData(data);
		result.setTotal(total);
		return result;
	}

	public static <T> Result<T> failed() {
		return result(ApiResultCode.SYSTEM_EXECUTION_ERROR.getCode(), ApiResultCode.SYSTEM_EXECUTION_ERROR.getMsg(), null);
	}

	public static <T> Result<T> failed(String msg) {
		return result(ApiResultCode.SYSTEM_EXECUTION_ERROR.getCode(), msg, null);
	}

	public static <T> Result<T> judge(boolean status) {
		if (status) {
			return success();
		} else {
			return failed();
		}
	}

	public static <T> Result<T> failed(ResultCode resultCode) {
		return result(resultCode.getCode(), resultCode.getMsg(), null);
	}

	public static <T> Result<T> failed(ResultCode resultCode, String msg) {
		return result(resultCode.getCode(), msg, null);
	}

	private static <T> Result<T> result(ResultCode resultCode, T data) {
		return result(resultCode.getCode(), resultCode.getMsg(), data);
	}

	private static <T> Result<T> result(String code, String msg, T data) {
		Result<T> result = new Result<>();
		result.setCode(code);
		result.setData(data);
		result.setMsg(msg);
		return result;
	}


	public static boolean isSuccess(Result<?> result) {
		return result != null && ApiResultCode.SUCCESS.getCode().equals(result.getCode());
	}
}
