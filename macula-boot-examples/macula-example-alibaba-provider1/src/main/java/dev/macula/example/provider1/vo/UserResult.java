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

package dev.macula.example.provider1.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

/**
 * Provider 用户接口的请求与响应模型。
 *
 * @author rain
 * @since 5.0.0
 */

@Data
@Schema(name = "用户信息")
public class UserResult {

    @Schema(name = "用户名", description = "用户名称，用于登陆")
    String userName;
    @Schema(name = "密码", description = "MD5加密")
    String password;
    @Schema(name = "出生日期", description = "ISO8601格式")
    Date birthday = new Date();
    @Schema(name = "年龄", description = "年龄")
    Integer age;
}
