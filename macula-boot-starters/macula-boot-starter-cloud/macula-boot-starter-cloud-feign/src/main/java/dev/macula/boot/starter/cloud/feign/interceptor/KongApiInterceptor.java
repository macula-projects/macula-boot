/*
 * Copyright (c) 2022 Macula
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

package dev.macula.boot.starter.cloud.feign.interceptor;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import dev.macula.boot.constants.SecurityConstants;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.AllArgsConstructor;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static java.time.ZoneOffset.UTC;

/**
 * {@code KongApiInterceptor} KONG认证的API访问拦截签名
 *
 * @author rain
 * @since 2022/7/27 08:59
 */
@AllArgsConstructor
public class KongApiInterceptor implements RequestInterceptor {
    private static final DateTimeFormatter RFC_7231_FORMATTER = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O").withLocale(Locale.ENGLISH);
    private String username;
    private String secret;

    @Override
    public void apply(RequestTemplate requestTemplate) {
        // 时间
        String date = RFC_7231_FORMATTER.format(ZonedDateTime.now(UTC));

        // 请求体摘要
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        byte[] hash = messageDigest.digest(requestTemplate.body());
        String digest = "SHA-256=" + Base64.encode(hash);
        // 签名，计算方法为 hmac_sha_256(key, 日期 + 方法 + 路径 + 摘要)
        HMac hmacSha256 = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes());
        String signature = hmacSha256.digestBase64("date: " + date + "\n" + requestTemplate.method() + " " + requestTemplate.url() + " HTTP/1.1\ndigest: " + digest, false);
        // HMAC 授权
        String authorization = "hmac username=\"" + username + "\", algorithm=\"hmac-sha256\", headers=\"date request-line digest\", signature=\"" + signature + "\"";

        requestTemplate.header(SecurityConstants.AUTHORIZATION_KEY, authorization);
        requestTemplate.header("Date", date);
        requestTemplate.header("Digest", digest);
    }
}
