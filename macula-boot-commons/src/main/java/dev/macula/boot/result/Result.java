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
        return failed(resultCode.getCode(), resultCode.getMsg(), data);
    }

    public static <T> Result<T> failed(String code, String msg, T data) {
        // data是错误原因
        Result<T> result = new Result<>();
        result.setSuccess(false);
        result.setCode(code);
        result.setMsg(msg);
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
