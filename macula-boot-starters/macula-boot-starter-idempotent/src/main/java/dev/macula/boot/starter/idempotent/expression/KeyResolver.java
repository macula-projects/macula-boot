/*
 * Copyright (c) 2022 Macula
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

package dev.macula.boot.starter.idempotent.expression;

import org.aspectj.lang.JoinPoint;
import dev.macula.boot.starter.idempotent.annotation.Idempotent;

/**
 * @author lengleng
 * @since 2020/9/25
 * <p>
 * 唯一标志处理器
 */
public interface KeyResolver {

    /**
     * 解析处理 key
     *
     * @param idempotent 接口注解标识
     * @param point      接口切点信息
     * @return 处理结果
     */
    String resolver(Idempotent idempotent, JoinPoint point);

}
