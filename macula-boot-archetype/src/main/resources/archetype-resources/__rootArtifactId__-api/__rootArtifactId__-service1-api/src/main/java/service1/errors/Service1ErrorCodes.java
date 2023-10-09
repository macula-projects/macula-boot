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

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.service1.errors;

import dev.macula.boot.result.ResultCode;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * {@code Service1ErrorCodes} is
 *
 * @author rain
 * @since 2023/9/18 15:14
 */
@AllArgsConstructor
@NoArgsConstructor
public enum Service1ErrorCodes implements ResultCode {

    BIZ_APP_ERROR("C0003", "APP请求失败");

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
