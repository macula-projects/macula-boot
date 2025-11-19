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

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作日志DTO
 *
 * @author Gordian
 * @since 2025-11-19
 */
@Data
public class OperationLogDTO implements Serializable {

    private static final long serialVersionUID = 8935542565073711237L;

    /**
     * 应用名称
     */
    private String application;

    /**
     * 日志级别
     */
    private OperationLogLevel level;

    /**
     * 日志操作类型
     */
    private String operation;

    /**
     * 业务范围
     */
    private String scope;

    /**
     * 模块名称
     */
    private String module;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 请求端ip
     */
    private String clientIp;
    /**
     * 请求方法
     */
    private String requestMethod;
    /**
     * 请求URI
     */
    private String requestUri;

    /**
     * 方法名
     */
    private String method;

    /**
     * 操作提交的参数
     */
    private Map<String, Object> parameters;

    /**
     * 操作返回结果
     */
    private Object result;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 执行时间(毫秒)
     */
    private Long executionTimeMillis;

    /**
     * 异常信息
     */
    private String exception;

    /**
     * traceId
     */
    private String traceId;

    /**
     * requestId
     */
    private String requestId;

}