/*
 * Copyright (c) 2023 Macula
 *   macula.dev, China
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.macula.boot.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应消息报文
 *
 * @param <T> 　T对象
 * @author pangu
 */
@Data
public class Result<T> implements Serializable {

    /**
     * 私有构造函数，防止外部实例化
     */
    private Result() {
    }

    /**
     * 响应是否成功
     */
    private boolean success;

    /**
     * 响应码
     */
    private String code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 错误原因
     */
    private String cause;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 返回成功的响应结果，无数据
     *
     * @param <T> 数据类型
     * @return 成功的响应结果
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 返回成功的响应结果，包含数据
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功的响应结果
     */
    public static <T> Result<T> success(T data) {
        return success(data, ApiResultCode.SUCCESS.getMsg());
    }

    /**
     * 返回成功的响应结果，包含数据和自定义消息
     *
     * @param data 响应数据
     * @param msg  响应消息
     * @param <T>  数据类型
     * @return 成功的响应结果
     */
    public static <T> Result<T> success(T data, String msg) {
        Result<T> result = new Result<>();
        result.setSuccess(true);
        result.setCode(ApiResultCode.SUCCESS.getCode());
        result.setMsg(msg);
        result.setData(data);
        return result;
    }

    /**
     * 返回失败的响应结果，使用默认失败码
     *
     * @param <T> 数据类型
     * @return 失败的响应结果
     */
    public static <T> Result<T> failed() {
        return failed(ApiResultCode.FAILED);
    }

    /**
     * 返回失败的响应结果，使用指定的结果码
     *
     * @param resultCode 结果码
     * @param <T>        数据类型
     * @return 失败的响应结果
     */
    public static <T> Result<T> failed(ResultCode resultCode) {
        return failed(resultCode, null);
    }

    /**
     * 返回失败的响应结果，使用指定的结果码和原因
     *
     * @param resultCode 结果码
     * @param cause      失败原因
     * @param <T>        数据类型
     * @return 失败的响应结果
     */
    public static <T> Result<T> failed(ResultCode resultCode, String cause) {
        return failed(resultCode.getCode(), resultCode.getMsg(), cause);
    }

    /**
     * 返回失败的响应结果，使用指定的码、消息和原因
     *
     * @param code 响应码
     * @param msg  响应消息
     * @param cause 失败原因
     * @param <T>  数据类型
     * @return 失败的响应结果
     */
    public static <T> Result<T> failed(String code, String msg, String cause) {
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMsg(msg);
        result.setCause(cause);
        return result;
    }

    /**
     * 返回失败的响应结果，使用指定的码和消息
     *
     * @param code 响应码
     * @param msg  响应消息
     * @param <T>  数据类型
     * @return 失败的响应结果
     */
    public static <T> Result<T> failed(String code, String msg) {
        return failed(code, msg, null);
    }

    /**
     * 根据状态返回成功或失败的响应结果
     *
     * @param status 状态，true表示成功，false表示失败
     * @param <T>    数据类型
     * @return 对应的响应结果
     */
    public static <T> Result<T> judge(boolean status) {
        if (status) {
            return success();
        } else {
            return failed();
        }
    }
}
