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
     * 服务ID,一般是服务名称
     */
    private String serviceId;

    /**
     * 日志类型
     */
    private OperationLogTypeEnum logType;

    /**
     * 日志操作类型
     */
    private String operation;

    /**
     * 层级信息
     */
    private String layer;

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
     * 请求方式
     */
    private String requestMode;
    /**
     * 请求URI
     */
    private String requestUri;

    /**
     * 方法名
     */
    private String method;

    /**
     * 操作提交的数据
     */
    private Map<String, Object> param;

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
     * 执行时间
     */
    private Long costTime;

    /**
     * 操作者
     */
    private String operationUser;

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