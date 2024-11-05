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

package dev.macula.boot.starter.cloud.alibaba.config;

import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 延迟注册服务到 Nacos 配置
 *
 * @author Gordian
 */
@Configuration
@ConditionalOnBean({NacosServiceRegistry.class, Registration.class})
@ConditionalOnProperty(prefix = "spring.cloud.nacos.discovery", name = "register-enabled", havingValue = "false")
public class NacosDelayRegistrationAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.cloud.nacos.discovery", name = "register-delayed", havingValue = "true")
    public NacosDelayRegistrationListener nacosDelayRegistrationListener(
            NacosServiceRegistry nacosServiceRegistry, Registration registration) {
        return new NacosDelayRegistrationListener(nacosServiceRegistry, registration);
    }

}
