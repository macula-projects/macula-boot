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

package dev.macula.boot.starter.operationlog;

import lombok.Getter;

/**
 * 操作日志级别枚举
 *
 * @author Gordian
 * @since 2025-11-19
 */
@Getter
public enum OperationLogLevel {

    /**
     * 信息日志级别
     */
    INFO("info", "信息"),

    /**
     * 警告日志级别
     */
    WARN("warn", "警告"),

    /**
     * 错误日志级别
     */
    ERROR("error", "错误");

    /**
     * 代码
     */
    private final String code;

    /**
     * 描述
     */
    private final String description;

    OperationLogLevel(String code, final String description) {
        this.code = code;
        this.description = description;
    }

}