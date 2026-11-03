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

package dev.macula.boot.starter.security.authorization;

import dev.macula.boot.security.ResourceAccessDenied;
import org.springframework.security.access.AccessDeniedException;

/**
 * 资源凭证校验拒绝异常，供现有 Web 异常处理识别并返回 HTTP 403/A0312。
 *
 * @author Rain
 * @since 6.1.0
 */
public class PasskeyAccessDeniedException extends AccessDeniedException implements ResourceAccessDenied {

    /**
     * 创建拒绝异常，消息不应包含签名密钥、完整凭证或受保护数据。
     * @param message 可对外展示的失败说明
     */
    public PasskeyAccessDeniedException(String message) {
        super(message);
    }
}
