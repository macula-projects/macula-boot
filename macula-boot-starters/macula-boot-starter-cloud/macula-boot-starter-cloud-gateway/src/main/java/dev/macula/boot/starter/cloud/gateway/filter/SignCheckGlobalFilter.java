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

package dev.macula.boot.starter.cloud.gateway.filter;

import cn.hutool.core.codec.PercentCodec;
import cn.hutool.core.net.RFC3986;
import cn.hutool.core.util.HexUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.starter.cloud.gateway.config.GatewayProperties;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.crypto.CryptoService;
import dev.macula.boot.starter.cloud.gateway.utils.RequestBodyUtils;
import dev.macula.boot.starter.cloud.gateway.utils.ResponseUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * {@code SignCheckGlobalFilter} 防篡改、防重放拦截器
 * <pre>
 * 请求头包含：
 * signature：
 *      GET请求签名sha256(path+param1=value1&param2=value2...+key+timestamp+nonce)
 *      POST请求签名sha256(path+SHA-256=+base64(sha256(body))+key+timestamp+nonce)
 * timestamp：时间戳
 * nonce：防重放随机串
 * algorithm：签名算法，支持(MD5，SHA-1，SHA-256)
 * sm4-key：SM4公钥加密(随机key)
 * </pre>
 *
 * @author rain
 * @since 2023/10/22 18:28
 */
@Slf4j
@RefreshScope
@RequiredArgsConstructor
public class SignCheckGlobalFilter implements GlobalFilter, Ordered {
    private final CryptoService cryptoService;
    private final GatewayProperties properties;
    private final RedisTemplate<String, Object> redisTemplate;

    private final PercentCodec urlEncoder = RFC3986.QUERY;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        URI uri = request.getURI();

        // 添加是否签名标志
        response.getHeaders().add(GatewayConstants.SIGN_SWITCH, String.valueOf(properties.isSignSwitch()));

        // 判断是否有签名参数，有则进行加验签操作，无则跳过
        // GlobalFilter里面不带URL不带路由前缀，需要获取原始的请求
        PathContainer pathContainer =
            exchange.getRequiredAttribute(ServerWebExchangeUtils.GATEWAY_PREDICATE_PATH_CONTAINER_ATTR);

        String path = pathContainer != null ? pathContainer.value() : uri.getPath();

        HttpMethod method = request.getMethod();
        String signature = request.getHeaders().getFirst(GatewayConstants.SIGNATURE_NAME);
        String timestamp = request.getHeaders().getFirst(GatewayConstants.TIMESTAMP_NAME);
        String nonce = request.getHeaders().getFirst(GatewayConstants.NONCE_NAME);
        String sm4Key = request.getHeaders().getFirst(GatewayConstants.SM4_KEY);
        if (StrUtil.isEmpty(signature) || StrUtil.isEmpty(timestamp) || StrUtil.isEmpty(nonce) || StrUtil.isEmpty(
            sm4Key)) {
            // 如果开启了接口强制验签则需要并且URL是属于需要签名的，则返回验签失败的错误
            PathMatcher pathMatcher = new AntPathMatcher();
            if (properties.isSignSwitch() && properties.isForceSign() && properties.getProtectUrls().getSign().stream()
                .anyMatch(s -> pathMatcher.match(s, path))) {

                return ResponseUtils.writeOkErrorInfo(response, ApiResultCode.API_SIGN_PARAMS_NOT_EXIST,
                    "缺少签名参数头: PATH=" + path);
            }
            return chain.filter(exchange);
        }

        // 1. 校验时间戳：服务器时间和请求时间相差超过300秒，校验不通过
        long timeCheck = (System.currentTimeMillis() - Long.parseLong(timestamp)) / (1000);
        if (timeCheck >= GatewayConstants.DEFAULT_TIMESTAMP_BTW) {
            log.info("timestamp: " + (System.currentTimeMillis()) / (1000));
            return ResponseUtils.writeOkErrorInfo(response, ApiResultCode.API_SIGN_ERROR, "时间戳超时");
        }

        // 2. 校验nonce参数：该参数不能在Redis中出现
        if (Boolean.TRUE.equals(redisTemplate.hasKey(CacheConstants.GATEWAY_NONCE_KEY + nonce))) {
            log.info("nonce:" + nonce);
            return ResponseUtils.writeOkErrorInfo(response, ApiResultCode.API_SIGN_ERROR, "禁止重复请求");
        } else {
            redisTemplate.opsForValue().set(CacheConstants.GATEWAY_NONCE_KEY + nonce, "x",
                Duration.ofSeconds(GatewayConstants.DEFAULT_TIMESTAMP_BTW));
        }

        // 3. 校验签名
        // GET签名体= path+param1=value1&param2=value2...+key+timestamp+nonce
        // POST签名体 = path+param1=value1&param2=value2...+SHA-256=+base64(sha256(body))+key+timestamp+nonce
        MultiValueMap<String, String> requestQueryParams = request.getQueryParams();
        String params = requestQueryParamsToString(requestQueryParams);
        log.debug("GET OR DELETE ciphertext  parameters： " + params);

        byte[] bodyBytes = null;
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            // 从请求里获取Post请求体，并对内容摘要
            bodyBytes = RequestBodyUtils.getBody(exchange);
            if (bodyBytes != null && bodyBytes.length > 0) {
                params = params + "SHA-256=" + HexUtil.encodeHexStr(DigestUtil.sha256(bodyBytes));
                log.debug("PUT OR POST params： " + params);
            }
        }

        // 校验
        sm4Key = cryptoService.decryptSm4Key(sm4Key);
        String plain = path + params + sm4Key + timestamp + nonce;
        String algorithm = request.getHeaders().getFirst(GatewayConstants.ALGORITHM_NAME);
        if (StrUtil.isEmpty(algorithm)) {
            algorithm = GatewayConstants.DEFAULT_ALGORITHM;
        }
        String newSignature = DigestUtil.digester(algorithm).digestHex(plain);
        log.debug("plain: " + plain);
        log.debug("newSignature: " + newSignature);
        if (!newSignature.equalsIgnoreCase(signature)) {
            return ResponseUtils.writeOkErrorInfo(response, ApiResultCode.API_SIGN_ERROR, "签名验证不通过");
        } else {
            // post请求的body需要解析之后再次封装，否则请求会报错
            if (bodyBytes != null) {
                ServerHttpRequest newRequest = RequestBodyUtils.rewriteRequestBody(exchange, bodyBytes);
                exchange = exchange.mutate().request(newRequest).build();
            }
        }
        return chain.filter(exchange);
    }

    private String requestQueryParamsToString(MultiValueMap<String, String> requestQueryParams) {
        if (requestQueryParams != null && !requestQueryParams.isEmpty()) {
            // 使用TreeMap对入参按照key排序，非空参数值才参与签名
            Map<String, List<String>> sortedParams = new TreeMap<>(requestQueryParams);
            List<String> paramsArray = new ArrayList<>();
            for (String key : sortedParams.keySet()) {
                String value = requestQueryParams.get(key).get(0);
                if (StrUtil.isNotEmpty(value)) {
                    // 由于客户端会把+编码为%20，后端收到的是空格，需要调整
                    String temp = key + "=" + value;
                    paramsArray.add(temp);
                }
            }

            StringBuilder params = new StringBuilder();
            for (int i = 0; i < paramsArray.size(); i++) {
                if (i == paramsArray.size() - 1) {
                    params.append(paramsArray.get(i));
                } else {
                    params.append(paramsArray.get(i)).append("&");
                }
            }
            return params.toString();
        }
        return "";
    }

    @Override
    public int getOrder() {
        return -8;
    }
}
