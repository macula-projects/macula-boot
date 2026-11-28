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

package dev.macula.boot.starter.cloud.gateway.security;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;

/**
 * 依据 System 发布的应用接口权限快照验证已认证应用。
 *
 * @author Rain
 * @since 6.1.0
 */
public final class ApplicationPermissionAuthorizer {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private ApplicationPermissionAuthorizer() {
    }

    /**
     * 使用可信凭据记录的归属租户与应用 ID 查询完整授权快照。
     *
     * @param redis System Redis
     * @param exchange 当前网关请求
     * @param credential 已验证的凭据记录
     * @return 是否有匹配授权
     */
    public static boolean isAllowed(RedisTemplate<String, ?> redis, ServerWebExchange exchange,
            Map<String, String> credential) {
        String ownerTenant = credential.get(GlobalConstants.TENANT_ID_NAME);
        String applicationId = credential.get(CacheConstants.APIKEY_APPLICATION_ID_ATTRIBUTE);
        if (ownerTenant == null || !ownerTenant.matches("[1-9][0-9]*")
                || applicationId == null || !applicationId.matches("[1-9][0-9]*")) {
            return false;
        }

        Object snapshot = redis.opsForValue().get(
                CacheConstants.APPLICATION_PERMISSIONS_CACHE_PREFIX + ownerTenant + ":" + applicationId);
        if (!(snapshot instanceof List<?> rules) || rules.stream().anyMatch(rule -> !(rule instanceof String))) {
            return false;
        }

        String entrancePath = exchange.getAttribute(GatewayConstants.APPLICATION_ENTRANCE_PATH_ATTRIBUTE);
        if (entrancePath == null) {
            return matchesRules(rules, exchange, originalPath(exchange));
        }
        return matchesRules(rules, exchange, entrancePath);
    }

    /** 兼容独立使用过滤器的调用方，优先读取框架保存的改写前地址。 */
    private static String originalPath(ServerWebExchange exchange) {
        Set<URI> originalUrls = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ORIGINAL_REQUEST_URL_ATTR);
        return originalUrls == null || originalUrls.isEmpty()
                ? exchange.getRequest().getURI().getPath() : originalUrls.iterator().next().getPath();
    }

    /** 用请求方法与受信的入口路径匹配已授权规则。 */
    private static boolean matchesRules(List<?> rules, ServerWebExchange exchange, String entrancePath) {
        String methodPrefix = exchange.getRequest().getMethod().name() + ":";
        return rules.stream().map(String.class::cast)
                .anyMatch(rule -> rule.startsWith(methodPrefix)
                        && PATH_MATCHER.match(rule.substring(methodPrefix.length()), entrancePath));
    }
}
