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

package dev.macula.boot.starter.web.handler;

import dev.macula.boot.starter.web.annotation.Sensitive;

/**
 * 自定义字段脱敏策略。
 *
 * <p>由序列化器通过无参构造器创建，不从 Spring 容器获取。实例可能被 Jackson 缓存并跨线程复用，
 * 实现应保持无状态或保证线程安全。null 和空串沿用序列化器的快捷处理，不调用本策略。</p>
 *
 * @author Rain
 * @since 6.1.0
 */
public interface SensitiveHandler {
    /**
     * 自定义脱敏处理方法
     * @param value 原文
     * @param sensitive 注解内容
     * @return 脱敏后内容
     */
    String handle(String value, Sensitive sensitive);
}
