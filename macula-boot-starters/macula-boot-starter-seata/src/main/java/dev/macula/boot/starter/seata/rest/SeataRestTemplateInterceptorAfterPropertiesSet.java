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

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * {@code SeataRestTemplateInterceptorAfterPropertiesSet} 设置RestTemplate
 *
 * @author rain
 * @since 2023/4/23 13:36
 */
@RequiredArgsConstructor
public class SeataRestTemplateInterceptorAfterPropertiesSet implements InitializingBean {

    private final Collection<RestTemplate> restTemplates;

    private final SeataRestTemplateInterceptor seataRestTemplateInterceptor;

    @Override
    public void afterPropertiesSet() {
        if (this.restTemplates != null) {
            for (RestTemplate restTemplate : restTemplates) {
                List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>(restTemplate.getInterceptors());
                interceptors.add(this.seataRestTemplateInterceptor);
                restTemplate.setInterceptors(interceptors);
            }
        }
    }

}
