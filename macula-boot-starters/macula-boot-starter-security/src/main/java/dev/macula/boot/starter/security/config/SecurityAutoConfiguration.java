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
import dev.macula.boot.starter.security.web.PasskeyArgumentResolverPostProcessor;
import dev.macula.boot.starter.security.passkey.HmacPasskeyService;
import dev.macula.boot.starter.security.passkey.PasskeyService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import javax.servlet.http.HttpServletRequest;

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

    /**
     * Supplies the default expiration clock, replaceable for application time policies.
     * @return UTC clock
     */
    @Bean
    @ConditionalOnProperty(name = "macula.security.passkey.enabled", havingValue = "true")
    @ConditionalOnMissingBean(Clock.class)
    Clock passkeyClock() {
        return Clock.systemUTC();
    }

    /**
     * Supplies the default credential implementation without requiring Web Starter.
     * @param properties security settings
     * @param clock expiration clock
     * @return replaceable credential service and response issuer
     */
    @Bean
    @ConditionalOnProperty(name = "macula.security.passkey.enabled", havingValue = "true")
    @ConditionalOnMissingBean(PasskeyService.class)
    PasskeyService passkeyService(SecurityProperties properties, Clock clock) {
        return new HmacPasskeyService(properties.getPasskey(), clock);
    }

    /**
     * 在 MVC 参数绑定后接入资源凭证校验，服务和配置均延迟到请求处理时获取。
     * @param services 可替换的凭证服务提供器
     * @param properties 安全配置提供器
     * @return MVC 参数解析装饰器的基础设施处理器
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(PasskeyArgumentResolverPostProcessor.class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static PasskeyArgumentResolverPostProcessor passkeyArgumentResolverPostProcessor(ObjectProvider<PasskeyService> services,
        ObjectProvider<SecurityProperties> properties) {
        return new PasskeyArgumentResolverPostProcessor(services::getIfAvailable,
            () -> properties.getObject().getPasskey().isEnabled());
    }

    @Bean
    SecurityInnerAspect securityInnerAspect(HttpServletRequest request) {
        return new SecurityInnerAspect(request);
    }
}
