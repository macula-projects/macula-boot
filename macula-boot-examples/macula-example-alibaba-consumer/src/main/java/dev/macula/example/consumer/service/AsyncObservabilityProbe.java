/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
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
package dev.macula.example.consumer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 为示例链路提供可重复验证的受管异步日志。
 *
 * @author Rain
 * @since 6.1.0
 */
@Component
@Slf4j
public class AsyncObservabilityProbe {

    /**
     * 在受管异步执行器中记录与请求关联的日志。
     *
     * @param name 请求名称
     */
    @Async
    public void logEcho(String name) {
        log.info("consumer managed-async echo by {}", name);
    }
}
