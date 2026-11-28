/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
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

package dev.macula.boot.starter.cloud.gateway.filter;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 HMAC 签发缓存与 Bearer 用户缓存隔离。
 *
 * @author Rain
 * @since 6.1.0
 */
class ApplicationJwtCacheIsolationTest {

    /** 同租户、主体和可选 jti 仍按认证来源区分。 */
    @Test
    void hmacCannotReadCachedBearerJwt() {
        for (var claims : List.<Map<String, Object>>of(Map.of("tenantId", 12L, "sub", "subject"),
                Map.of("tenantId", 12L, "sub", "subject", "jti", "token-id"))) {
            var principal = new DefaultOAuth2AuthenticatedPrincipal("subject", claims, List.of());
            assertThat(AddJwtGlobalFilter.buildKey(principal, true))
                    .isNotEqualTo(AddJwtGlobalFilter.buildKey(principal));
        }
    }
}
