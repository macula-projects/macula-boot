/*
 * Copyright (c) 2024 Macula
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

package dev.macula.boot.exception;

import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.ResultCode;
import lombok.Getter;

/**
 * {@code BizCheckException} 是服务层对外的统一异常
 *
 * @author rain
 * @since 2024/1/8 17:25
 */
@Getter
public class BizCheckException extends MaculaException {

    private final String code;
    private final String msg;

    public BizCheckException(ResultCode resultCode, String exceptionMessage) {
        this(resultCode.getCode(), resultCode.getMsg(), exceptionMessage);
    }

    public BizCheckException(String code, String msg, String exceptionMessage) {
        // message用于用户设置抛出错误详情，例如：当前价格-5，小于0
        super(exceptionMessage);
        this.code = code;
        this.msg = msg;
    }

    public BizCheckException(String exceptionMessage) {
        this(ApiResultCode.BIZ_CHECK_ERROR, exceptionMessage);
    }
}
