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
package dev.macula.boot.starter.tinyid.config;

import dev.macula.boot.starter.feign.interceptor.KongApiInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * 仅在 TinyID Feign 子上下文中启用的应用签名配置。
 *
 * @author Rain
 * @since 6.1.0
 */
public class TinyIdFeignClientConfiguration {

    @Bean
    public KongApiInterceptor tinyIdApiInterceptor(@Value("${macula.cloud.app-key}") String appKey,
        @Value("${macula.cloud.secret-key}") String secretKey) {
        return new KongApiInterceptor(appKey, secretKey);
    }
}
