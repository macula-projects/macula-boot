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

package dev.macula.boot.starter.seata.rest;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;

/**
 * {@code SeataRestTemplateAutoConfiguration} RestTemplate Seata配置
 *
 * @author rain
 * @since 2023/4/23 13:37
 */
@AutoConfiguration
@ConditionalOnClass(RestTemplate.class)
public class SeataRestTemplateAutoConfiguration {

    @Bean
    public SeataRestTemplateInterceptor seataRestTemplateInterceptor() {
        return new SeataRestTemplateInterceptor();
    }

    @Bean
    public SeataRestTemplateInterceptorAfterPropertiesSet seataRestTemplateInterceptorConfiguration(
        SeataRestTemplateInterceptor interceptor, @Nullable Collection<RestTemplate> restTemplates) {
        return new SeataRestTemplateInterceptorAfterPropertiesSet(restTemplates, interceptor);
    }
}