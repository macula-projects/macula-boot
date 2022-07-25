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

package dev.macula.boot.starter.cloud.feign.config;

import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import dev.macula.boot.starter.cloud.feign.codec.OpenFeignErrorDecoder;
import dev.macula.boot.starter.cloud.feign.interceptor.HeaderRelayInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * <p>
 * <b>FeignConfiguration</b> OpenFeign的自动配置
 * </p>
 *
 * @author Rain
 * @since 2022-02-25
 */
@AutoConfiguration
public class FeignAutoConfiguration {
    @Bean
    public RequestInterceptor headerRelayInterceptor() {
        return new HeaderRelayInterceptor();
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new OpenFeignErrorDecoder();
    }
}
