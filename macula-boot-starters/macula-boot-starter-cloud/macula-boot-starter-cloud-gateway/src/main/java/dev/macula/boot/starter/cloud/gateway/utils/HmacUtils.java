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
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;

import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * {@code HmacUtils} 基于Hmac的签名校验助手
 *
 * @author rain
 * @since 2023/2/20 12:38
 */
public class HmacUtils {
    /**
     * 校验请求的签名是否正确，并验证URL是否匹配
     *
     * @param exchange      请求Exchagne
     * @param redisTemplate 秘钥提取Redis
     * @return 错误信息或者200，200表示成功
     */
    public static Result<String> checkSign(ServerWebExchange exchange, RedisTemplate redisTemplate, String path) {
        try {
            ServerHttpRequest request = exchange.getRequest();

            String token = request.getHeaders().getFirst(GlobalConstants.AUTHORIZATION_KEY);

            String username = StrUtil.subBetween(token, "hmac username=\"", "\",");
            if (StrUtil.isBlank(username)) {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "username缺少，验签失败");
            }

            Map<String, String> apps =
                redisTemplate.opsForHash().entries(GlobalConstants.SECURITY_SYSTEM_APPS + username);
            // 验证ak/sk
            String secretKey = apps.get(GlobalConstants.SECURITY_SYSTEM_APPS_SECRIT_KEY);
            if (StrUtil.isBlank(secretKey)) {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "秘钥未配置, 验签失败");
            }

            // 检查URL是否允许，默认允许[多个URL表达式用逗号隔开，'GET:/i18n-base/v1/users/*, POST:/xxx/**']
            String permitUrls = apps.get(GlobalConstants.SECURITY_SYSTEM_APPS_PERMIT_URLS);
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
            long pastTime = DateUtil.between(requestDate, new Date(), DateUnit.MINUTE);
            if (pastTime > 5) {
                return Result.failed(ApiResultCode.AKSK_ACCESS_FORBIDDEN, "请求已经过期");
            }

            String method = request.getMethodValue();
            String uri = request.getURI().getPath();

            if (StrUtil.isNotBlank(request.getURI().getQuery())) {
                uri = uri + "?" + request.getURI().getQuery();
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
                byte[] body = RequestBodyUtils.getBody(exchange);
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
