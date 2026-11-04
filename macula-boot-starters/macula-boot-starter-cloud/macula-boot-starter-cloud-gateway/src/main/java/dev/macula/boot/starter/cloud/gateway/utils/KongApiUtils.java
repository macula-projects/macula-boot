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
import dev.macula.boot.constants.SecurityConstants;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import java.security.MessageDigest;
import java.util.Date;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * {@code KongApiUtils} HMAC 凭据识别、签名验证及可信身份读取工具。
 *
 * @author rain
 * @since 2023/2/20 12:38
 */
public class KongApiUtils {

    /**
     * 从 Authorization Header 识别待验签的 HMAC 凭据，普通请求返回 null。
     * 这里只按协议前缀分流，不代表凭据完整或已经通过验签；HMAC 不接受 Query token。
     *
     * @param exchange 当前请求
     * @return 原始 HMAC Authorization 值，或 null
     */
    @Nullable
    public static String resolveHmacToken(ServerWebExchange exchange) {
        String token = exchange.getRequest().getHeaders().getFirst(SecurityConstants.AUTHORIZATION_KEY);
        return StrUtil.startWithIgnoreCase(token, GatewayConstants.HMAC_AUTH_PREFIX) ? token : null;
    }

    /**
     * 读取仅由 HMAC 认证过滤器在验签成功后构造的可信 principal。
     * 不从原始请求或单独的验签成功结果临时构造身份。
     *
     * @param exchange 当前请求
     * @return 已验证的 HMAC 应用身份
     * @throws BadCredentialsException 缺少可信 principal 或属性类型非法
     */
    public static OAuth2AuthenticatedPrincipal verifiedPrincipal(ServerWebExchange exchange) {
        Object value = exchange.getAttribute(GatewayConstants.HMAC_PRINCIPAL_ATTRIBUTE);
        if (value instanceof OAuth2AuthenticatedPrincipal principal) {
            return principal;
        }
        throw new BadCredentialsException("Missing verified HMAC principal");
    }

    /**
     * 判断 Header 是否包含可解析的 HMAC 应用名，供请求体缓存及清理入口识别协议。
     * 此检查不验证签名，不能用作认证成功的依据。
     *
     * @param exchange 当前请求
     * @return 是否包含 HMAC 应用名
     */
    public static boolean isKongApiRequest(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        String token = request.getHeaders().getFirst(SecurityConstants.AUTHORIZATION_KEY);
        String username = StrUtil.subBetween(token, GatewayConstants.HMAC_USERNAME_PREFIX, "\",");
        return StrUtil.isNotBlank(username);
    }

    /**
     * 使用调用方提供的密钥校验请求时间与 HMAC 签名，不读取应用缓存或检查租户、URL 许可。
     * 本方法不发布身份；验签成功仅表示请求与密钥匹配，完整应用准入由认证过滤器负责。
     *
     * @param exchange 携带 HMAC Authorization 和 Date 的请求；请求体须提前缓存
     * @param secretKey 服务端取得的应用密钥
     * @return 时间及签名的校验结果
     */
    public static Result<String> checkSign(ServerWebExchange exchange, String secretKey) {
        try {
            ServerHttpRequest request = exchange.getRequest();
            String token = resolveHmacToken(exchange);
            if (StrUtil.isBlank(secretKey)) {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "秘钥未配置, 验签失败");
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
                return Result.success();
            } else {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "验签失败");
            }
        } catch (Exception e) {
            return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, e.getMessage());
        }
    }
}
