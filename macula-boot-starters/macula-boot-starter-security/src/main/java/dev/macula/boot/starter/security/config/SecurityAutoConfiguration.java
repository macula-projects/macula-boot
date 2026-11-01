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
import dev.macula.boot.starter.security.authorization.CheckOwnerAdvisor;
import dev.macula.boot.starter.security.passkey.HmacPasskeyService;
import dev.macula.boot.starter.security.passkey.PasskeyService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Role;
import java.time.Clock;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.annotation.AnnotationTemplateExpressionDefaults;

import jakarta.servlet.http.HttpServletRequest;

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
     * Enables method-security annotation templates unless the application provides its own defaults.
     *
     * @return annotation template defaults
     */
    @Bean
    @ConditionalOnMissingBean(AnnotationTemplateExpressionDefaults.class)
    static AnnotationTemplateExpressionDefaults templateExpressionDefaults() {
        return new AnnotationTemplateExpressionDefaults();
    }

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
     * Registers the unified ownership advisor without eagerly creating security properties.
     * @param services replaceable credential service
     * @param properties security settings provider
     * @return resource authorization advisor
     */
    @Bean
    @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
    @ConditionalOnMissingBean(CheckOwnerAdvisor.class)
    static CheckOwnerAdvisor checkOwnerAdvisor(ObjectProvider<PasskeyService> services,
        ObjectProvider<SecurityProperties> properties) {
        return new CheckOwnerAdvisor(services::getObject, () -> properties.getObject().getPasskey().isEnabled());
    }

    @Bean
    SecurityInnerAspect securityInnerAspect(HttpServletRequest request) {
        return new SecurityInnerAspect(request);
    }
}
