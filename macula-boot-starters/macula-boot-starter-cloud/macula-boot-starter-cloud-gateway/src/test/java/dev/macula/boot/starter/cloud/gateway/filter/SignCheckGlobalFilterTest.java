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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 验证签名过滤器的密钥解密错误只返回固定响应。
 *
 * @author Rain
 * @since 6.1.0
 */
@SuppressWarnings("unchecked")
class SignCheckGlobalFilterTest {
    /** 密钥无法解开时结束请求，且不输出底层异常。 */
    @Test
    void hidesDecryptFailureAndStopsTheChain() {
        CryptoService service = mock(CryptoService.class);
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(service.decryptSm4Key("wrapped")).thenThrow(new IllegalArgumentException("fixture-private-detail"));
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/demo")
            .header(GatewayConstants.SIGNATURE_NAME, "signature")
            .header(GatewayConstants.TIMESTAMP_NAME, Long.toString(System.currentTimeMillis()))
            .header(GatewayConstants.NONCE_NAME, "fixture-nonce")
            .header(GatewayConstants.SM4_KEY, "wrapped"));
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_PREDICATE_PATH_CONTAINER_ATTR,
            PathContainer.parsePath("/demo"));

        new SignCheckGlobalFilter(service, new GatewayProperties(), redis).filter(exchange, chain).block();

        assertThat(exchange.getResponse().getBodyAsString().block()).contains(ApiResultCode.API_CRYPTO_ERROR.getCode())
            .doesNotContain("fixture-private-detail");
        verifyNoInteractions(chain);
    }
}
