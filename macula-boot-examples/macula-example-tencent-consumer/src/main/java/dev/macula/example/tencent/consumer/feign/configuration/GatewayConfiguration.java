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

package dev.macula.example.tencent.consumer.feign.configuration;

import dev.macula.boot.starter.feign.interceptor.KongApiInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class GatewayConfiguration {

    @Value("${gateway.username}")
    private String username;
    @Value("${gateway.secret}")
    private String secret;
    @Value("${gateway.appKey}")
    private String appKey;

    @Bean
    KongApiInterceptor gatewayInterceptor() {
        return new KongApiInterceptor(username, secret, appKey);
    }
}
