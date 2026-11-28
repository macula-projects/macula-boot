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

package dev.macula.boot.starter.cloud.gateway.filter;

import cn.hutool.crypto.digest.DigestUtil;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.starter.cloud.gateway.config.GatewayProperties;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.crypto.CryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.server.PathContainer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证签名过滤器的 nonce 防重放缓存和密钥解密错误响应。
 *
 * @author Rain
 * @since 6.1.0
 */
@SuppressWarnings("unchecked")
class SignCheckGlobalFilterTest {
    private static final String NONCE_KEY = "macula:cloud:gateway:nonce:fixture-nonce";

    /** 密钥无法解开时结束请求，且不输出底层异常。 */
    @Test
    void hidesDecryptFailureAndStopsTheChain() {
        CryptoService service = mock(CryptoService.class);
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        when(service.decryptSm4Key("wrapped")).thenThrow(new IllegalArgumentException("fixture-private-detail"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        var exchange = signedRequest(System.currentTimeMillis());

        new SignCheckGlobalFilter(service, new GatewayProperties(), redis).filter(exchange, chain).block();

        verifyNoInteractions(redis);
        assertThat(exchange.getResponse().getBodyAsString().block()).contains(ApiResultCode.API_CRYPTO_ERROR.getCode())
            .doesNotContain("fixture-private-detail");
        verifyNoInteractions(chain);
    }

    /** 并发重复请求只完成一次，下次尝试不会覆盖登记或延长 TTL。 */
    @Test
    void reservesNonceAtomicallyAfterValidSignature() {
        CryptoService service = mock(CryptoService.class);
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(service.decryptSm4Key("wrapped")).thenReturn("fixture-key");
        when(redis.opsForValue()).thenReturn(values);
        Duration ttl = Duration.ofSeconds(GatewayConstants.DEFAULT_TIMESTAMP_BTW);
        var reserved = new AtomicBoolean();
        when(values.setIfAbsent(NONCE_KEY, "x", ttl)).thenAnswer(call -> reserved.compareAndSet(false, true));
        var filter = new SignCheckGlobalFilter(service, new GatewayProperties(), redis);
        long timestamp = System.currentTimeMillis();
        var first = signedRequest(timestamp);
        var second = signedRequest(timestamp);
        var forwarded = new AtomicInteger();
        GatewayFilterChain chain = exchange -> {
            forwarded.incrementAndGet();
            return exchange.getResponse().setComplete();
        };

        Mono.when(Mono.defer(() -> filter.filter(first, chain)).subscribeOn(Schedulers.parallel()),
                Mono.defer(() -> filter.filter(second, chain)).subscribeOn(Schedulers.parallel())).block();

        assertThat(forwarded).hasValue(1);
        assertThat(first.getResponse().getBodyAsString().block() + second.getResponse().getBodyAsString().block())
                .contains(ApiResultCode.API_SIGN_ERROR.getCode(), "禁止重复请求");
        verify(values, times(2)).setIfAbsent(NONCE_KEY, "x", ttl);
    }

    /**
     * 创建带完整签名头和网关入口路径的测试请求。
     *
     * @param timestamp 测试签名时间戳
     * @return 测试用请求及响应
     */
    private MockServerWebExchange signedRequest(long timestamp) {
        String signature = DigestUtil.sha256Hex("/demofixture-key" + timestamp + "fixture-nonce");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/demo")
            .header(GatewayConstants.SIGNATURE_NAME, signature)
            .header(GatewayConstants.TIMESTAMP_NAME, Long.toString(timestamp))
            .header(GatewayConstants.NONCE_NAME, "fixture-nonce")
            .header(GatewayConstants.SM4_KEY, "wrapped"));
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_PREDICATE_PATH_CONTAINER_ATTR,
            PathContainer.parsePath("/demo"));

        return exchange;
    }
}
