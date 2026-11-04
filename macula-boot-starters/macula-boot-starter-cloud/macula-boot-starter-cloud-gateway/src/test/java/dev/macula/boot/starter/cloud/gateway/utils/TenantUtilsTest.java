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

package dev.macula.boot.starter.cloud.gateway.utils;

import dev.macula.boot.constants.CacheConstants;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 归属租户最长前缀匹配、大小写及未知租户处理的回归测试。
 *
 * @author Rain
 * @since 6.1.0
 */
class TenantUtilsTest {
    @Test
    void resolvesGatewayTenantUsingLongestCaseInsensitiveMatchingCode() {
        var cache = KongApiUtilsTest.redis("12", null);
        when(cache.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_TENANTS))
            .thenReturn(Map.of("macula", "1", "macula-samples", "12"));
        assertThat(TenantUtils.resolveGatewayTenantId(cache, "macula-samples-gateway")).isEqualTo(12L);
        assertThat(TenantUtils.resolveGatewayTenantId(cache, "macula-cloud-gateway")).isEqualTo(1L);
        assertThat(TenantUtils.resolveGatewayTenantId(cache, "other-gateway")).isEqualTo(-1L);
        assertThat(TenantUtils.resolveGatewayTenantId(cache, "")).isEqualTo(-1L);
        assertThat(TenantUtils.resolveGatewayTenantId(cache, null)).isEqualTo(-1L);
        assertThat(TenantUtils.resolveGatewayTenantId(cache, "MACULA-SAMPLES-gateway")).isEqualTo(12L);
        when(cache.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_TENANTS))
            .thenReturn(Map.of("MACULA", "1", "MaCuLa-SaMpLeS", "12"));
        assertThat(TenantUtils.resolveGatewayTenantId(cache, "macula-samples-GATEWAY")).isEqualTo(12L);
    }

    @Test
    void missingAndInvalidTenantCacheResolveToUnknownWithoutBroaderFallback() {
        var cache = KongApiUtilsTest.redis("12", null);
        when(cache.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_TENANTS))
            .thenReturn(Map.of());
        assertThat(TenantUtils.resolveGatewayTenantId(cache, "macula-samples-gateway")).isEqualTo(-1L);
        when(cache.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_TENANTS))
            .thenReturn(Map.of("macula", "1", "macula-samples", "invalid", "", "12"));
        assertThat(TenantUtils.resolveGatewayTenantId(cache, "macula-samples-gateway")).isEqualTo(-1L);
    }

    /** 归属一致才准入，principal 使用独立认证租户，未配置时回退归属。 */
    @Test
    void admitsOwnerAndResolvesSeparateAuthenticationTenant() {
        var cache = KongApiUtilsTest.redis("12", null);
        assertThat(TenantUtils.checkTenant(cache, "macula-samples-gateway", applicationTenants("12", "7")).getData()).isEqualTo(7L);
        assertThat(TenantUtils.checkTenant(cache, "macula-samples-gateway", applicationTenants("12", null)).getData()).isEqualTo(12L);
        assertThat(TenantUtils.checkTenant(cache, "macula-samples-gateway", applicationTenants("12", "")).getData()).isEqualTo(12L);
        assertThat(TenantUtils.checkTenant(cache, "macula-samples-gateway", applicationTenants("7", "12")).isSuccess()).isFalse();
        assertThat(TenantUtils.checkTenant(cache, "macula-samples-gateway", applicationTenants(null, "12")).isSuccess()).isFalse();
        assertThat(TenantUtils.checkTenant(cache, "unknown-gateway", applicationTenants("-1", "12")).isSuccess()).isFalse();
    }

    /** 网关归属必须明确，非法缓存租户继续交给认证调用方处理。 */
    @Test
    void rejectsUnknownGatewayAndMalformedTenantIds() {
        var cache = KongApiUtilsTest.redis("12", null);
        assertThat(TenantUtils.checkTenant(cache, "unknown-gateway", applicationTenants("12", "7")).isSuccess()).isFalse();
        assertThat(TenantUtils.checkTenant(cache, null, applicationTenants(null, null)).isSuccess()).isFalse();
        assertThatThrownBy(() -> TenantUtils.checkTenant(cache, "macula-samples-gateway", applicationTenants("invalid", "7")))
            .isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> TenantUtils.checkTenant(cache, "macula-samples-gateway", applicationTenants("12", "invalid")))
            .isInstanceOf(NumberFormatException.class);
    }
    /** 按当前应用 Map 接口构造租户字段，缺失字段使用真正缺省值验证。 */
    private Map<String, String> applicationTenants(String ownerTenant, String authTenant) {
        Map<String, String> apps = new HashMap<>();
        if (ownerTenant != null) {
            apps.put(dev.macula.boot.constants.GlobalConstants.TENANT_ID_NAME, ownerTenant);
        }
        if (authTenant != null) {
            apps.put(CacheConstants.SECURITY_SYSTEM_APPS_AUTH_TENANT_ID, authTenant);
        }
        return apps;
    }

}
