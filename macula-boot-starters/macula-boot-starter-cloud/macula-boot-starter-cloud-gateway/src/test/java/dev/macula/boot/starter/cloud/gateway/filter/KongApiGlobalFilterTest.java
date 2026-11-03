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

import dev.macula.boot.starter.cloud.gateway.utils.KongApiUtilsTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 普通 HMAC 入口必须在调用下游之前执行网关归属校验。
 *
 * @author Rain
 * @since 6.1.0
 */
class KongApiGlobalFilterTest {
    @Test
    void rejectsOtherTenantBeforeInvokingChain() {
        var exchange = KongApiUtilsTest.request("GET", "/orders", "app", "");
        AtomicBoolean invoked = new AtomicBoolean();
        KongApiGlobalFilter filter = new KongApiGlobalFilter(KongApiUtilsTest.redis("7", "12"));
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
        filter.filter(exchange, ignored -> {
                invoked.set(true);
                return Mono.empty();
            }).block();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(invoked).isFalse();
    }

    @Test
    void permitsMatchingTenantAndRejectsUnknownGateway() {
        AtomicBoolean invoked = new AtomicBoolean();
        KongApiGlobalFilter filter = new KongApiGlobalFilter(KongApiUtilsTest.redis("12", "7"));
        ReflectionTestUtils.setField(filter, "applicationName", "macula-samples-gateway");
        filter.filter(KongApiUtilsTest.request("GET", "/orders", "app", ""), ignored -> {
                invoked.set(true);
                return Mono.empty();
            }).block();
        assertThat(invoked).isTrue();
        invoked.set(false);
        ReflectionTestUtils.setField(filter, "applicationName", "unknown-gateway");
        var unknown = KongApiUtilsTest.request("GET", "/orders", "app", "");
        filter.filter(unknown, ignored -> {
                invoked.set(true);
                return Mono.empty();
            }).block();
        assertThat(invoked).isFalse();
        assertThat(unknown.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
