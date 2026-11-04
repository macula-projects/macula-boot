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

import java.util.List;

import cn.hutool.core.util.StrUtil;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenErrors;
import org.springframework.web.server.ServerWebExchange;

/**
 * API Key 的输入边界检查与请求级可信身份读取。
 *
 * @author Rain
 * @since 6.1.0
 */
public final class ApiKeyUtils {

    private static final String BEARER_SK_PREFIX = "Bearer sk-";

    private ApiKeyUtils() {
    }

    /**
     * 解析待验证的密钥；普通请求返回 null，API Key 输入冲突或格式非法时拒绝。
     * 此返回值仅表示输入通过格式检查，不代表已认证。
     *
     * @param exchange 当前请求
     * @return 待验证的 API Key，或 null
     * @throws OAuth2AuthenticationException API Key 输入非法
     */
    @Nullable
    public static String resolveApiKey(ServerWebExchange exchange) {
        List<String> headers = exchange.getRequest().getHeaders().get(SecurityConstants.AUTHORIZATION_KEY);
        List<String> query = exchange.getRequest().getQueryParams().get(SecurityConstants.QUERY_AUTHORIZATION_KEY);
        boolean headerCandidate = headers != null && headers.stream().anyMatch(value -> value != null
            && StrUtil.startWithIgnoreCase(value.strip().replaceAll("\\s+", " "), BEARER_SK_PREFIX));
        boolean queryCandidate = query != null && query.stream().anyMatch(value -> value != null
            && StrUtil.startWithIgnoreCase(value.strip(), "sk-"));
        if (!headerCandidate && !queryCandidate) {
            return null;
        }
        // 不能只取第一个值：重复来源或 Header/Query 冲突会让各认证阶段解释不同的凭据。
        if (headers != null && query != null || headers != null && headers.size() != 1
            || query != null && query.size() != 1) {
            throw invalidInput();
        }
        String token = RequestUtils.getHeaderOrQueryToken(exchange);
        if (token == null || !token.matches("(?i)Bearer sk-[a-zA-Z0-9._~+/-]+=*")) {
            throw invalidInput();
        }
        return token.substring(SecurityConstants.TOKEN_PREFIX.length());
    }

    /**
     * 读取仅由 API Key 验证过滤器写入的可信身份。
     *
     * @param exchange 当前请求
     * @return 已验证身份
     * @throws BadCredentialsException 缺少可信身份
     */
    public static OAuth2AuthenticatedPrincipal verifiedPrincipal(ServerWebExchange exchange) {
        Object value = exchange.getAttribute(GatewayConstants.APIKEY_PRINCIPAL_ATTRIBUTE);
        if (value instanceof OAuth2AuthenticatedPrincipal principal
                && GatewayConstants.APIKEY_AUTH_TYPE.equals(principal.getAttribute(GatewayConstants.AUTH_TYPE_ATTRIBUTE))) {
            return principal;
        }
        throw new BadCredentialsException("Missing verified API Key identity");
    }

    /**
     * 构造不包含凭据值的输入错误，避免异常被记录或输出时泄漏密钥。
     *
     * @return 标准 Bearer 输入错误
     */
    private static OAuth2AuthenticationException invalidInput() {
        return new OAuth2AuthenticationException(BearerTokenErrors.invalidRequest("Invalid API Key credentials"));
    }
}
