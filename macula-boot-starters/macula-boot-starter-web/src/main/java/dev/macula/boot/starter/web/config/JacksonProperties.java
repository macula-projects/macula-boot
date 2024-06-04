/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.macula.boot.starter.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * <b>WebProperties</b> Macula WEB模块的配置
 * </p>
 *
 * @author Rain
 * @since 2024/3/25
 */
@Data
@ConfigurationProperties(prefix = "macula.jackson")
public class JacksonProperties {
    /**
     * 长数字是否统一转为字符串(Long/BigInteger/BigDecimal)
     */
    private boolean longToString = true;

    /**
     * NULL是否转为空串等
     */
    private boolean nullToEmpty = false;

    /**
     * LocalDateTime的格式配置
     */
    private String localDateTimeFormat;

    /**
     * LocalDate的格式配置
     */
    private String localDateFormat;

    /**
     * LocalTime的格式配置
     */
    private String localTimeFormat;
}
