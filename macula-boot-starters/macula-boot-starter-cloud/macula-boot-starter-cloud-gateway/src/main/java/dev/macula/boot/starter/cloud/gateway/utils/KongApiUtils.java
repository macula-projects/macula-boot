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

package dev.macula.boot.starter.cloud.gateway.utils;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.Comparator;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * {@code KongApiUtils} 基于Hmac的签名校验助手
 *
 * @author rain
 * @since 2023/2/20 12:38
 */
public class KongApiUtils {

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
        String tenantId = tenants.entrySet().stream()
            .filter(entry -> StrUtil.isNotBlank(entry.getKey()) && StrUtil.startWithIgnoreCase(applicationName, entry.getKey()))
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

    public static boolean isKongApiRequest(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String token = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_KEY);
        String username = StrUtil.subBetween(token, "hmac username=\"", "\",");
        return StrUtil.isNotBlank(username);
    }

    /**
     * 校验请求的签名是否正确，并验证URL是否匹配
     *
     * @param exchange         请求Exchagne
     * @param sysRedisTemplate 秘钥提取Redis
     * @param path 请求路径
     * @return 错误信息或者200，200表示成功
     */
    public static Result<String> checkSign(ServerWebExchange exchange, RedisTemplate<String, ?> sysRedisTemplate,
        String path) {
        return checkSignForTenant(exchange, sysRedisTemplate, path, null);
    }

    /**
     * 根据网关应用名解析归属租户，并校验应用接入资格、签名及接口权限。
     *
     * @param exchange 当前请求
     * @param sysRedisTemplate System 的租户及应用缓存
     * @param path 带 HTTP 方法的权限路径
     * @param applicationName 网关的 spring.application.name
     * @return 验证结果；未匹配到网关租户时拒绝接入
     */
    public static Result<String> checkSign(ServerWebExchange exchange, RedisTemplate<String, ?> sysRedisTemplate,
        String path, String applicationName) {
        return checkSignForTenant(exchange, sysRedisTemplate, path,
            resolveGatewayTenantId(sysRedisTemplate, applicationName));
    }

    /**
     * 校验应用归属、签名及接口权限，成功后在请求属性中保存可信身份。
     *
     * @param exchange 当前请求
     * @param sysRedisTemplate 应用凭据缓存
     * @param path 带 HTTP 方法的权限路径
     * @param gatewayTenantId 网关所属租户，null 表示保持旧接入行为
     * @return 验证结果；失败时不保留可信身份
     */
    private static Result<String> checkSignForTenant(ServerWebExchange exchange, RedisTemplate<String, ?> sysRedisTemplate,
        String path, Long gatewayTenantId) {
        exchange.getAttributes().remove(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE);
        try {
            ServerHttpRequest request = exchange.getRequest();

            String token = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_KEY);

            String username = StrUtil.subBetween(token, "hmac username=\"", "\",");
            if (StrUtil.isBlank(username)) {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "username缺少，验签失败");
            }

            Map<String, String> apps =
                sysRedisTemplate.<String, String>opsForHash().entries(CacheConstants.SECURITY_SYSTEM_APPS + username);

            // 验证ak/sk
            String secretKey = apps.get(CacheConstants.SECURITY_SYSTEM_APPS_SECRIT_KEY);
            if (StrUtil.isBlank(secretKey)) {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "秘钥未配置, 验签失败");
            }

            String ownerTenant = apps.get(GlobalConstants.TENANT_ID_NAME);
            Long ownerTenantId = StrUtil.isEmpty(ownerTenant) ? null : Long.valueOf(ownerTenant);
            if (gatewayTenantId != null && (gatewayTenantId == GatewayConstants.UNKNOWN_TENANT_ID
                || !gatewayTenantId.equals(ownerTenantId))) {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "应用归属租户不允许接入本网关");
            }
            String authTenant = apps.get(CacheConstants.SECURITY_SYSTEM_APPS_AUTH_TENANT_ID);
            Long authTenantId = StrUtil.isEmpty(authTenant) ? ownerTenantId : Long.valueOf(authTenant);

            // 检查URL是否允许，默认允许[多个URL表达式用逗号隔开，'GET:/i18n-base/v1/users/*, POST:/xxx/**']
            String permitUrls = apps.get(CacheConstants.SECURITY_SYSTEM_APPS_PERMIT_URLS);
            if (StrUtil.isNotBlank(permitUrls)) {
                PathMatcher pathMatcher = new AntPathMatcher();
                if (Arrays.stream(permitUrls.split(",")).noneMatch(pattern -> pathMatcher.match(pattern, path))) {
                    return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "不允许访问该接口");
                }
            }

            String signature = StrUtil.subBetween(token, "signature=\"", "\"");
            if (StrUtil.isBlank(signature)) {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "signature不能为空, 验签失败");
            }

            String algorithm = StrUtil.subBetween(token, "algorithm=\"", "\"");

            // 校验date是否过期，默认5分钟过期
            String dateStr = request.getHeaders().getFirst("Date");
            Date requestDate = DatePattern.HTTP_DATETIME_FORMAT.parse(dateStr);
            long pastTime = DateUtil.between(requestDate, new Date(), DateUnit.SECOND);
            if (pastTime > GatewayConstants.DEFAULT_TIMESTAMP_BTW) {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "请求已经过期");
            }

            String method = request.getMethod().name();
            String uri = request.getURI().getPath();

            if (StrUtil.isNotBlank(request.getURI().getRawQuery())) {
                uri = uri + "?" + request.getURI().getRawQuery();
            }

            // 签名，计算方法为 hmac_sha_256(key, 日期 + 方法 + 路径 + 摘要)
            HMac hmac = new HMac(HmacAlgorithm.HmacSHA256, secretKey.getBytes(UTF_8));
            if ("hmac-sha1".equals(algorithm)) {
                hmac = new HMac(HmacAlgorithm.HmacSHA1, secretKey.getBytes(UTF_8));
            }
            if ("hmac-sha384".equals(algorithm)) {
                hmac = new HMac(HmacAlgorithm.HmacSHA384, secretKey.getBytes(UTF_8));
            }
            if ("hmac-sha512".equals(algorithm)) {
                hmac = new HMac(HmacAlgorithm.HmacSHA512, secretKey.getBytes(UTF_8));
            }

            String signatureNew;
            if ("GET".equals(method) || "DELETE".equals(method)) {
                signatureNew = hmac.digestBase64("date: " + dateStr + "\n" + method + " " + uri + " HTTP/1.1", false);

            } else {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                byte[] body = RequestUtils.getBody(exchange);
                if (body == null) {
                    body = new byte[0];
                }

                byte[] hash = messageDigest.digest(body);
                String digest = "SHA-256=" + Base64.encode(hash);
                signatureNew =
                    hmac.digestBase64("date: " + dateStr + "\n" + method + " " + uri + " HTTP/1.1\ndigest: " + digest,
                        false);
            }

            if (StrUtil.isNotBlank(signature) && signature.equals(signatureNew)) {
                exchange.getAttributes().put(GatewayConstants.HMAC_IDENTITY_ATTRIBUTE,
                    new HmacIdentity(username, authTenantId));
                return Result.success();
            } else {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "验签失败");
            }
        } catch (Exception e) {
            return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, e.getMessage());
        }
    }

    /**
     * 当前请求通过验签的应用及其有效业务租户。
     *
     * @param applicationCode 验签使用的应用编码
     * @param tenantId 业务租户，可为 null 以兼容旧无租户配置
     * @since 6.1.0
     */
    public record HmacIdentity(String applicationCode, Long tenantId) {
    }

}
