/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
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

package dev.macula.boot.starter.auditlog.test.vo;

import lombok.Data;

/**
 * <p>
 * <b>UserForm</b> 用户表单对象，用于测试审计日志参数处理
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
@Data
public class UserForm {

    /** 用户ID */
    private Long id;

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;

    /** 密码（敏感字段，会被排除） */
    private String password;

    /** 确认密码（敏感字段，会被排除） */
    private String confirmPassword;
}
