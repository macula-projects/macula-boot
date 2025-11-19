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

/**
 * 操作日志类型枚举
 *
 * @author Gordian
 * @since 2025-11-19
 */
public enum OperationLogTypeEnum {

    /**
     * 正常日志类型
     */
    NORMAL(1, "正常日志"),

    /**
     * 错误日志类型
     */
    ERROR(2, "错误日志");

    /**
     * 类型
     */
    private final Integer type;

    /**
     * 描述
     */
    private final String description;

    OperationLogTypeEnum(Integer type, final String description) {
        this.type = type;
        this.description = description;
    }

    /**
     * 获取操作类型
     *
     * @return
     */
    public Integer getType() {
        return this.type;
    }

    /**
     * 获取操作描述
     *
     * @return
     */
    public String getDescription() {
        return this.description;
    }
}