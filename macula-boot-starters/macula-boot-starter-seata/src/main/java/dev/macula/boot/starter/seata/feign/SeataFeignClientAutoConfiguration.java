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

package dev.macula.boot.starter.seata.feign;

import feign.Client;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * {@code SeataFeignClientAutoConfiguration} Feign Seat配置
 *
 * @author rain
 * @since 2023/4/23 11:22
 */
@AutoConfiguration
@ConditionalOnClass(Client.class)
public class SeataFeignClientAutoConfiguration {

    @Bean
    public static SeataFeignBuilderBeanPostProcessor seataFeignBuilderBeanPostProcessor() {
        return new SeataFeignBuilderBeanPostProcessor();
    }

    @Bean
    public SeataFeignRequestInterceptor seataFeignRequestInterceptor() {
        return new SeataFeignRequestInterceptor();
    }

}