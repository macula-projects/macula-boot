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

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;

/**
 * 操作日志事件监听器
 *
 * @author Gordian
 * @since 2025-11-19
 */
@Slf4j
public class OperationLogListener {

    /**
     * 处理操作日志事件
     *
     * @param event 操作日志事件
     */
    @Order
    @EventListener(OperationLogEvent.class)
    @Async
    public void saveOperationLog(OperationLogEvent event) {
        OperationLogDTO operationLog = event.getOperationLog();
        log.info("[OperationLog] {} ->\n {}", operationLog.getMethod(), JSONUtil.toJsonStr(operationLog));
    }

}