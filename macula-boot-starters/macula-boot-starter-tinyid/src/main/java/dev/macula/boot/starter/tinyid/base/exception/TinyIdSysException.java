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

package dev.macula.boot.starter.tinyid.base.exception;

import dev.macula.boot.exception.BizException;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.ResultCode;

/**
 * 携带统一结果码的 TinyID 系统异常，可由 BizException 异常处理器识别。
 * 
 * @author du_imba
 * @since 5.0.0
 */
public class TinyIdSysException extends BizException {

    /** 使用默认系统错误码创建异常。 */
    public TinyIdSysException() {
        this(ApiResultCode.SYS_ERROR, null);
    }

    /**
     * @param message 异常详情
     */
    public TinyIdSysException(String message) {
        this(ApiResultCode.SYS_ERROR, message);
    }

    /**
     * @param message 异常详情
     * @param cause 原始异常
     */
    public TinyIdSysException(String message, Throwable cause) {
        this(ApiResultCode.SYS_ERROR, message, cause);
    }

    /**
     * @param cause 原始异常，其文本作为异常详情
     */
    public TinyIdSysException(Throwable cause) {
        this(cause == null ? null : cause.toString(), cause);
    }

    /**
     * @param resultCode 结果码及对外消息，不得为 null
     * @param message 异常详情，与对外消息分开保存
     */
    public TinyIdSysException(ResultCode resultCode, String message) {
        super(resultCode, message);
    }

    /**
     * @param resultCode 结果码及对外消息，不得为 null
     * @param message 异常详情
     * @param cause 原始异常
     */
    public TinyIdSysException(ResultCode resultCode, String message, Throwable cause) {
        this(resultCode, message);
        initCause(cause);
    }
}
