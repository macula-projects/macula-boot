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

    /**
     * 请求成功
     */
    SUCCESS("00000", "请求成功"),
    
    /**
     * 请求失败
     */
    FAILED("99999", "请求失败"),

    // 访问异常
    /**
     * 未授权的访问
     */
    ACCESS_UNAUTHORIZED("A0301", "未授权的访问"),
    
    /**
     * 访问令牌无效或已过期
     */
    TOKEN_INVALID_OR_EXPIRED("A0311", "访问令牌无效或已过期"),
    
    /**
     * 令牌没有访问授权
     */
    TOKEN_ACCESS_FORBIDDEN("A0312", "令牌没有访问授权"),
    
    /**
     * 基于AKSK的访问签名无效
     */
    AKSK_ACCESS_FORBIDDEN("A0340", "基于AKSK的访问签名无效"),
    
    /**
     * 参数校验错误
     */
    VALIDATE_ERROR("A0400", "参数校验错误"),

    // 业务异常
    /**
     * 业务异常
     */
    BIZ_ERROR("B0001", "业务异常"),
    
    /**
     * 系统异常
     */
    SYS_ERROR("B0002", "系统异常"),
    
    /**
     * 业务检查异常
     */
    BIZ_CHECK_ERROR("B0003", "业务检查异常"),
    
    /**
     * Response返回包装失败
     */
    RESPONSE_PACK_ERROR("B0400", "Response返回包装失败"),
    
    /**
     * 接口加解密异常
     */
    API_CRYPTO_ERROR("B0401", "接口加解密异常"),
    
    /**
     * 加密接口缺少KEY
     */
    API_CRYPTO_KEY_NOT_EXIST("B0402", "加密接口缺少KEY"),
    
    /**
     * 接口防篡改防重放校验未通过
     */
    API_SIGN_ERROR("B0411", "接口防篡改防重放校验未通过"),
    
    /**
     * 接口防篡改防重放缺少必备参数
     */
    API_SIGN_PARAMS_NOT_EXIST("B0412", "接口防篡改防重放缺少必备参数"),

    /**
     * 限流
     */
    FLOW_LIMITING("C0401", "限流"),
    
    /**
     * 系统降级
     */
    DEGRADATION("C0402", "系统降级");

    /**
     * 响应码
     */
    private String code;

    /**
     * 响应消息
     */
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
