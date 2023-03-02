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

import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * {@code SecretCondition} 使用Secret生成JWT的条件
 *
 * @author rain
 * @since 2022/7/25 13:42
 */
public class SecretCondition extends SpringBootCondition {
    @Override
    public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
        ConditionMessage.Builder message = ConditionMessage.forCondition("Secret Key Condition");
        Environment environment = context.getEnvironment();
        String issuerUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri");
        String jwkSetUri = environment.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri");
        String publicKeyLocation =
            environment.getProperty("spring.security.oauth2.resourceserver.jwt.public-key-location");
        if (StringUtils.hasText(jwkSetUri)) {
            return ConditionOutcome.noMatch(message.found("jwk-set-uri property").items(jwkSetUri));
        }
        if (StringUtils.hasText(issuerUri)) {
            return ConditionOutcome.noMatch(message.found("issuer-uri property").items(issuerUri));
        }
        if (StringUtils.hasText(publicKeyLocation)) {
            return ConditionOutcome.noMatch((message.found(" public-key-location property")).items(publicKeyLocation));
        }
        return ConditionOutcome.match(message.foundExactly("secret property"));
    }
}
