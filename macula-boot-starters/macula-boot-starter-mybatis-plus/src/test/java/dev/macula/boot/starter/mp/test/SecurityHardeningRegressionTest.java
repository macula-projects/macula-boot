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


package dev.macula.boot.starter.mp.test;

import dev.macula.boot.context.TenantContextHolder;
import dev.macula.boot.starter.mp.config.MyBatisPlusAutoConfiguration;
import dev.macula.boot.starter.mp.config.MyBatisPlusProperties;
import dev.macula.boot.starter.security.utils.SecurityUtils;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 5.x ROOT 租户切换回归，保持 Java 8 API。
 * @author Rain
 * @since 5.0.21
 */
class SecurityHardeningRegressionTest {

    /** 构造普通认证身份，验证请求租户不能覆盖认证租户。 */
    @BeforeEach
    void authenticate() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("tenantId", 12L);
        OAuth2AuthenticatedPrincipal principal = new DefaultOAuth2AuthenticatedPrincipal("alice", attributes, Collections.emptyList());
        OAuth2AccessToken token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test", Instant.EPOCH, Instant.MAX);
        SecurityContextHolder.getContext().setAuthentication(new BearerTokenAuthentication(principal, token, Collections.emptyList()));
    }

    /** 避免身份和请求租户污染后续测试。 */
    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clearCurrentTenantId();
    }

    /** 仅经认证 ROOT 可以使用请求租户，普通身份使用认证租户。 */
    @Test
    void restrictsTenantSwitchToAuthenticatedRoot() {
        TenantContextHolder.setCurrentTenantId(99L);
        TenantLineInnerInterceptor tenant = (TenantLineInnerInterceptor)new MyBatisPlusAutoConfiguration()
            .mybatisPlusInterceptor(new MyBatisPlusProperties()).getInterceptors().get(0);
        assertThat(tenant.getTenantLineHandler().getTenantId().toString()).isEqualTo("12");
        TestingAuthenticationToken root = new TestingAuthenticationToken("root", "test", "ROLE_ROOT");
        SecurityContextHolder.getContext().setAuthentication(root);
        assertThat(tenant.getTenantLineHandler().getTenantId().toString()).isEqualTo("99");
        root.setAuthenticated(false);
        assertThat(SecurityUtils.isRoot()).isFalse();
    }

}
