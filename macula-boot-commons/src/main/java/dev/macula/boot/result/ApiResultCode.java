/*
 * Copyright (c) 2022 Macula
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

import dev.macula.boot.result.ResultCode;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 返回码实现
 *
 * @author pangu
 */

@AllArgsConstructor
@NoArgsConstructor
public enum ApiResultCode implements ResultCode, Serializable {

    SUCCESS("10000", "请求成功"),
    FAILED("10001", "请求失败"),

    ACCESS_UNAUTHORIZED("10010", "未授权的访问"),
    TOKEN_INVALID_OR_EXPIRED("10011", "访问令牌无效或已过期"),
    TOKEN_ACCESS_FORBIDDEN("10012", "令牌没有访问授权"),

    BIZ_ERROR("10021", "业务异常"),
    SYS_ERROR("10022", "系统异常"),
    VALIDATE_ERROR("10023", "参数校验错误"),
    RESPONSE_PACK_ERROR("10024", "Response返回包装失败"),
    FLOW_LIMITING("10051", "限流"),
    DEGRADATION("10052", "系统降级");

    private String code;

    private String msg;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        return "code=" + this.code + ", msg=" + this.msg;
    }

}
