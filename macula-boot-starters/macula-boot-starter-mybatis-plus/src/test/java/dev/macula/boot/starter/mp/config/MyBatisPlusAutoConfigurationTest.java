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


package dev.macula.boot.starter.mp.config;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import dev.macula.boot.context.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import dev.macula.boot.starter.mp.interceptor.ParameterizedDataPermissionInterceptor;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthentication;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 MyBatis 租户选择点的 ROOT 授权边界及原有插件规则。
 *
 * @author Rain
 * @since 6.1.0
 */
class MyBatisPlusAutoConfigurationTest {
    /** 清除测试的请求和认证上下文。 */
    @AfterEach
    void clear() {
        TenantContextHolder.clearCurrentTenantId();
        SecurityContextHolder.clearContext();
    }

    /** 请求租户只允许 ROOT 使用，普通用户仍使用认证租户。 */
    @Test
    void onlyRootCanSwitchTenant() {
        var interceptor = new MyBatisPlusAutoConfiguration().mybatisPlusInterceptor(new MyBatisPlusProperties());
        var tenant = (TenantLineInnerInterceptor)interceptor.getInterceptors().get(0);
        TenantContextHolder.setCurrentTenantId(99L);
        var authorities = AuthorityUtils.createAuthorityList("ROLE_USER");
        var principal = new DefaultOAuth2AuthenticatedPrincipal("alice", Map.of("tenantId", 12L), authorities);
        var accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "test", Instant.EPOCH, Instant.MAX);
        SecurityContextHolder.getContext().setAuthentication(new BearerTokenAuthentication(principal, accessToken, authorities));
        assertThat(tenant.getTenantLineHandler().getTenantId().toString()).isEqualTo("12");
        authorities = AuthorityUtils.createAuthorityList("ROLE_ROOT");
        SecurityContextHolder.getContext().setAuthentication(new BearerTokenAuthentication(principal, accessToken, authorities));
        assertThat(tenant.getTenantLineHandler().getTenantId().toString()).isEqualTo("99");
        TenantContextHolder.clearCurrentTenantId();
        assertThat(tenant.getTenantLineHandler().getTenantId().toString()).isEqualTo("12");
        SecurityContextHolder.clearContext();
        assertThat(tenant.getTenantLineHandler().getTenantId().toString()).isEqualTo("1");
        assertThat(tenant.getTenantLineHandler().ignoreTable("users_tenant")).isFalse();
        assertThat(tenant.getTenantLineHandler().ignoreTable("users")).isTrue();
    }

    /** 关闭租户插件不应新增隐式隔离行为。 */
    @Test
    void honorsDisable() {
        var properties = new MyBatisPlusProperties();
        properties.setTenantEnable(false);
        assertThat(new MyBatisPlusAutoConfiguration().mybatisPlusInterceptor(properties).getInterceptors())
            .noneMatch(TenantLineInnerInterceptor.class::isInstance);
    }
    /** 单一工厂装配参数化插件，无额外数据源 Bean；关闭权限时跳过插件。 */
    @Test
    void installsOrDisablesPermissionInterceptor() {
        var runner = new ApplicationContextRunner().withUserConfiguration(MyBatisPlusAutoConfiguration.class);
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor.class).getInterceptors())
                .anyMatch(ParameterizedDataPermissionInterceptor.class::isInstance);
            assertThat(context).doesNotHaveBean("departmentDataScopeProvider");
        });
        runner.withPropertyValues("macula.mybatis-plus.data-permission-enable=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor.class).getInterceptors())
                .noneMatch(ParameterizedDataPermissionInterceptor.class::isInstance);
        });
    }
}
