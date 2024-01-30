/*
 * Copyright (c) 2024 Macula
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

package dev.macula.boot.starter.sleuth.config;

import brave.handler.MutableSpan;
import brave.handler.SpanHandler;
import brave.propagation.TraceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;

/**
 * {@code SleuthAutoConfiguration} Sleuth自动化配置
 *
 * @author rain
 * @since 2024/1/28 19:25
 */
@AutoConfiguration
@Slf4j
public class SleuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingClass("org.springframework.cloud.sleuth.zipkin2.EndpointLocator")
    public SpanHandler spanHandler() {
        // 如果没有引入zipkin，则输出到控制台
        return new SpanHandler() {
            @Override
            public boolean end(TraceContext context, MutableSpan span, Cause cause) {
                log.info(span.toString());
                return true;
            }
        };
    }
}
