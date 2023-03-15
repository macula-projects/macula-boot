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

package dev.macula.boot.starter.feign.interceptor;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.date.DateUtil;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import dev.macula.boot.constants.SecurityConstants;
import feign.Request;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * {@code KongApiInterceptor} KONG认证的API访问拦截签名
 *
 * @author rain
 * @since 2022/7/27 08:59
 */
@AllArgsConstructor
public class KongApiInterceptor implements RequestInterceptor {

    @NonNull
    private String username;
    @NonNull
    private String secret;
    private String appKey;

    public KongApiInterceptor(@NonNull String username, @NonNull String secret) {
        this.username = username;
        this.secret = secret;
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        String date = DateUtil.formatHttpDate(new Date());
        requestTemplate.header("Date", date);
        // 签名，计算方法为 hmac_sha_256(key, 日期 + 方法 + 路径)
        HMac hmacSha256 = new HMac(HmacAlgorithm.HmacSHA256, secret.getBytes());
        String authorization = null;
        String method = requestTemplate.method();
        if (Request.HttpMethod.POST.name().equals(method) || Request.HttpMethod.PUT.toString().equals(method)) {
            // 请求体摘要
            MessageDigest messageDigest = null;
            try {
                messageDigest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            byte[] hash = messageDigest.digest(requestTemplate.body() == null ? new byte[] {} : requestTemplate.body());
            String digest = "SHA-256=" + Base64.encode(hash);
            requestTemplate.header("Digest", digest);
            // 签名，计算方法为 hmac_sha_256(key, 日期 + 方法 + 路径 + 摘要)
            String signature = hmacSha256.digestBase64(
                "date: " + date + "\n" + method + " " + requestTemplate.url() + " HTTP/1.1\ndigest: " + digest, false);
            authorization =
                "hmac username=\"" + username + "\", algorithm=\"hmac-sha256\", headers=\"date request-line digest\", signature=\"" + signature + "\"";
        } else if (Request.HttpMethod.GET.name().equals(method) || Request.HttpMethod.DELETE.toString()
            .equals(method)) {
            // 签名，计算方法为 hmac_sha_256(key, 日期 + 方法 + 路径)
            String signature =
                hmacSha256.digestBase64("date: " + date + "\n" + method + " " + requestTemplate.url() + " HTTP/1.1",
                    false);
            authorization =
                "hmac username=\"" + username + "\", algorithm=\"hmac-sha256\", headers=\"date request-line\", signature=\"" + signature + "\"";
        }
        if (authorization != null) {
            requestTemplate.removeHeader(SecurityConstants.AUTHORIZATION_KEY);
            requestTemplate.header(SecurityConstants.AUTHORIZATION_KEY, authorization);
        }
        if (appKey != null) {
            requestTemplate.header("appkey", appKey);
        }
    }
}
