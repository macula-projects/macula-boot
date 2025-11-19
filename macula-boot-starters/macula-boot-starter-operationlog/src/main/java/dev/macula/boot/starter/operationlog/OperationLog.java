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

import java.lang.annotation.*;

/**
 * 操作日志注解
 *
 * @author Gordian
 * @since 2025-11-19
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /**
     * 操作类型
     *
     * @return
     */
    String operation() default OperationLogConstant.TYPE_SELECT;

    /**
     * 业务范围；controller,service,domain等
     */
    String scope() default OperationLogConstant.SCOPE_CONTROLLER;

    /**
     * 是否记录参数
     */
    boolean logParameters() default true;

    /**
     * 是否记录结果
     */
    boolean logResult() default false;

    /**
     * 模块名称
     */
    String module();

    /**
     * 描述
     */
    String description();

}