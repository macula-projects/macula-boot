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

package dev.macula.boot.starter.security.config;

import dev.macula.boot.starter.security.aspect.SecurityInnerAspect;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import jakarta.servlet.http.HttpServletRequest;

/**
 * <p>
 * <b>SecurityAutoConfiguration</b> 安全配置
 * </p>
 *
 * @author Rain
 * @since 2022-02-09
 */
@AutoConfiguration
@Import({ResourceServerConfiguration.class})
public class SecurityAutoConfiguration {
    @Bean
    SecurityInnerAspect securityInnerAspect(HttpServletRequest request) {
        return new SecurityInnerAspect(request);
    }
}
