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

import java.net.InetSocketAddress;
import java.util.Comparator;
import java.util.Map;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import dev.macula.boot.security.AccessRuleUtils;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;

/**
 * 解析网关归属租户，统一校验应用准入及认证业务租户。
 *
 * @author Rain
 * @since 6.1.0
 */
public final class TenantUtils {
    private TenantUtils() {
    }

    /**
     * 按网关应用名忽略大小写匹配租户编码，多个匹配时使用最长前缀。
     *
     * @param sysRedisTemplate System 的租户编码缓存
     * @param applicationName 网关的 spring.application.name
     * @return 租户 ID；应用名缺失、缓存未命中或 ID 非法时返回 -1
     */
    public static Long resolveGatewayTenantId(RedisTemplate<String, ?> sysRedisTemplate, String applicationName) {
        if (StrUtil.isBlank(applicationName)) {
            return GatewayConstants.UNKNOWN_TENANT_ID;
        }
        Map<String, String> tenants = sysRedisTemplate.<String, String>opsForHash()
                .entries(CacheConstants.SECURITY_SYSTEM_TENANTS);
        // 先选最长匹配；其 ID 非法时拒绝，不回退到更宽泛的租户前缀。
        String tenantId = tenants.entrySet().stream()
                .filter(entry -> StrUtil.isNotBlank(entry.getKey()) &&
                        StrUtil.startWithIgnoreCase(applicationName, entry.getKey()))
                .max(Comparator.comparingInt(entry -> entry.getKey().length()))
                .map(Map.Entry::getValue).orElse(null);
        if (StrUtil.isEmpty(tenantId)) {
            return GatewayConstants.UNKNOWN_TENANT_ID;
        }
        try {
            return Long.valueOf(tenantId);
        } catch (NumberFormatException exception) {
            return GatewayConstants.UNKNOWN_TENANT_ID;
        }
    }

    /**
     * 校验应用归属是否允许接入本网关，并解析写入 principal 的认证业务租户。
     * 归属租户只用于准入，认证租户可与归属不同；未配置认证租户时回退到归属。
     *
     * @param sysRedisTemplate System 的租户映射缓存
     * @param applicationName 网关的 spring.application.name
     * @param apps 应用缓存
     * @return 准入失败结果，或携带业务租户的成功结果
     * @throws NumberFormatException 缓存中的租户 ID 非法，由调用方按认证入口处理失败
     */
    public static Result<Long> checkTenant(RedisTemplate<String, ?> sysRedisTemplate, String applicationName,
            Map<String, String> apps) {
        Long gatewayTenantId = TenantUtils.resolveGatewayTenantId(sysRedisTemplate, applicationName);

        String ownerTenant = apps.get(GlobalConstants.TENANT_ID_NAME);
        String authTenant = apps.get(CacheConstants.SECURITY_SYSTEM_APPS_AUTH_TENANT_ID);

        Long ownerTenantId = StrUtil.isEmpty(ownerTenant) ? null : Long.valueOf(ownerTenant);
        // 应用名或映射无法确认归属时拒绝，不能跳过网关准入。
        if (gatewayTenantId != null && (gatewayTenantId == GatewayConstants.UNKNOWN_TENANT_ID
                || !gatewayTenantId.equals(ownerTenantId))) {
            return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "应用归属租户不允许接入本网关");
        }
        Long authTenantId = StrUtil.isEmpty(authTenant) ? ownerTenantId : Long.valueOf(authTenant);
        return Result.success(authTenantId);
    }


    /**
     * 集中执行应用及 API Key 的租户、URL、来源 IP 准入，成功后调用方才能发布可信身份。
     * 未配置的维度视为不限，已配置的 URL 与 IP 必须同时满足。
     *
     * @param exchange 当前请求，路径沿用调用入口所在的 StripPrefix 时点
     * @param sysRedisTemplate System 租户映射缓存
     * @param applicationName 网关应用名
     * @param apps 凭据缓存，permitIps 缺失或空白表示不限
     * @return 失败准入结果，或携带认证租户的成功结果
     */
    public static Result<Long> checkTenantAndUrls(ServerWebExchange exchange, RedisTemplate<String, ?> sysRedisTemplate,
            String applicationName, Map<String, String> apps) {
        // 公共入口集中判断，HMAC、API Key 及强制 HMAC 的清理端点不能绕过其中任一限制。
        Result<Long> tenant = TenantUtils.checkTenant(sysRedisTemplate, applicationName, apps);
        if (!tenant.isSuccess()) {
            return Result.failed(tenant.getCode(), tenant.getMsg(), tenant.getCause());
        }

        try {
            String permitUrls = apps.get(CacheConstants.SECURITY_SYSTEM_APPS_PERMIT_URLS);
            String path = exchange.getRequest().getMethod().name() + ":" + exchange.getRequest().getURI().getPath();
            PathMatcher matcher = new AntPathMatcher();
            boolean urlAllowed = AccessRuleUtils.matchesUrl(permitUrls, path, matcher::match);
            if (!urlAllowed) {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "不允许访问该接口");
            }

            String permitIps = apps.get(CacheConstants.SECURITY_SYSTEM_APPS_PERMIT_IPS);
            // 只使用直连对端；不能将调用方可伪造的 Forwarded/X-Forwarded-For 作为可信地址。
            InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
            byte[] address = remote == null || remote.getAddress() == null ? null : remote.getAddress().getAddress();
            boolean ipAllowed = AccessRuleUtils.matchesIp(permitIps, address);
            if (!ipAllowed) {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "不允许该来源 IP 访问");
            }
        } catch (IllegalArgumentException exception) {
            // 损坏或超长的非空权限缓存不能回退为不限，也不回显缓存中的配置值。
            return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "应用访问规则不合法");
        }
        return tenant;
    }
}
