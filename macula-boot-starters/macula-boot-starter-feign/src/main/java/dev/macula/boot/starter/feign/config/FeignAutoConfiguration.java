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

package dev.macula.boot.starter.feign.config;

import dev.macula.boot.starter.feign.codec.OpenFeignErrorDecoder;
import dev.macula.boot.starter.feign.interceptor.FeignHeaderRelayProperties;
import dev.macula.boot.starter.feign.interceptor.HeaderRelayInterceptor;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
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
    public RequestInterceptor headerRelayInterceptor(FeignHeaderRelayProperties properties) {
        return new HeaderRelayInterceptor(properties);
    }

    @Bean
    @RefreshScope
    public FeignHeaderRelayProperties feignHeaderRelayProperties() {
        return new FeignHeaderRelayProperties();
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new OpenFeignErrorDecoder();
    }
}
