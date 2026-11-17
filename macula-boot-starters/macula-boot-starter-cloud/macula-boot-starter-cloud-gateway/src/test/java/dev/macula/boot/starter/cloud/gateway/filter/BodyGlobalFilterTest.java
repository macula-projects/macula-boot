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

import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import io.netty.buffer.PooledByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.util.unit.DataSize;
import org.springframework.web.server.handler.ResponseStatusExceptionHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 固定原过滤器的分支、缓存短路和链构造语义，只验证新增聚合上限。
 *
 * @author Rain
 * @since 6.1.0
 */
class BodyGlobalFilterTest {
    private final BodyGlobalFilter filter = new BodyGlobalFilter(DataSize.ofBytes(4));

    /** 构造进入原有加密缓存分支的请求。 */
    private MockServerWebExchange exchange(Flux<DataBuffer> body, Long length) {
        var builder = MockServerHttpRequest.post("/demo").header("sm4-key", "encrypted").header("sym-alg", "SM4");
        if (length != null) {
            builder.header("Content-Length", length.toString());
        }
        return MockServerWebExchange.from(builder.body(body));
    }

    /** 构造实际长度独立于 Header 的缓冲。 */
    private DataBuffer buffer(String text) {
        return DefaultDataBufferFactory.sharedInstance.wrap(text.getBytes(StandardCharsets.UTF_8));
    }

    /** 使用标准 WebFlux 异常处理验证 413，不改变过滤器内部的 then 控制流。 */
    private void handleHttpErrors(MockServerWebExchange exchange, GatewayFilterChain chain) {
        filter.filter(exchange, chain).onErrorResume(failure ->
            new ResponseStatusExceptionHandler().handle(exchange, failure)).block();
    }

    /** 保留原链构造时机：先构造下游 Mono，聚合完成后才订阅它。 */
    @Test
    void constructsChainBeforeCachingAndSubscribesAfterwards() {
        var exchange = exchange(Flux.just(buffer("123")), null);
        AtomicInteger assembled = new AtomicInteger();
        AtomicInteger subscribed = new AtomicInteger();
        Mono<Void> result = filter.filter(exchange, current -> {
            assembled.incrementAndGet();
            assertThat((Object)current.getAttribute(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY)).isNull();
            return Mono.defer(() -> {
                subscribed.incrementAndGet();
                assertThat((byte[])current.getAttribute(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY))
                    .isEqualTo("123".getBytes(StandardCharsets.UTF_8));
                return Mono.empty();
            });
        });
        assertThat(assembled).hasValue(1);
        assertThat(subscribed).hasValue(0);
        result.block();
        assertThat(assembled).hasValue(1);
        assertThat(subscribed).hasValue(1);
    }

    /** 边界长度及空流保持 defaultIfEmpty/doOnNext/then 的原行为。 */
    @Test
    void acceptsWithinLimitAndEmptyBody() {
        for (String body : new String[] {"", "123", "1234"}) {
            var exchange = exchange(body.isEmpty() ? Flux.empty() : Flux.just(buffer(body)), null);
            AtomicInteger calls = new AtomicInteger();
            filter.filter(exchange, current -> Mono.defer(() -> {
                calls.incrementAndGet();
                assertThat((byte[])current.getAttribute(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY))
                    .isEqualTo(body.getBytes(StandardCharsets.UTF_8));
                return Mono.empty();
            })).block();
            assertThat(calls).hasValue(1);
        }
    }

    /** 流式超限产生标准 413，已构造的下游 Mono 不会被订阅。 */
    @Test
    void rejectsActualOversizeBeforeSubscribingDownstream() {
        for (Long length : new Long[] {null, 1L, 5L}) {
            var exchange = exchange(Flux.just(buffer("123"), buffer("45")), length);
            AtomicInteger assembled = new AtomicInteger();
            AtomicInteger subscribed = new AtomicInteger();
            handleHttpErrors(exchange, current -> {
                assembled.incrementAndGet();
                return Mono.fromRunnable(subscribed::incrementAndGet);
            });
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
            assertThat(assembled).hasValue(1);
            assertThat(subscribed).hasValue(0);
            assertThat((Object)exchange.getAttribute(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY)).isNull();
        }
    }

    /** 不新增 Content-Length 预判，按照实际读取的字节数限制聚合。 */
    @Test
    void keepsActualBodySemanticsWithLargeDeclaredLength() {
        var exchange = exchange(Flux.just(buffer("123")), 100L);
        AtomicInteger subscribed = new AtomicInteger();
        filter.filter(exchange, current -> Mono.fromRunnable(subscribed::incrementAndGet)).block();
        assertThat(subscribed).hasValue(1);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    /** 任意非 null 缓存对象均按原逻辑短路，不转换类型、不重复读取或重新限额。 */
    @Test
    void preservesExistingCacheShortCircuit() {
        for (Object cached : new Object[] {new Object(), new byte[5]}) {
            AtomicInteger reads = new AtomicInteger();
            var exchange = exchange(Flux.<DataBuffer>never().doOnSubscribe(subscription -> reads.incrementAndGet()), 100L);
            exchange.getAttributes().put(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY, cached);
            filter.filter(exchange, current -> {
                assertThat((Object)current.getAttribute(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY)).isSameAs(cached);
                return Mono.empty();
            }).block();
            assertThat(reads).hasValue(0);
        }
    }

    /** 普通请求、非缓存方法及 HMAC 的原分流条件不变。 */
    @Test
    void preservesMethodAndProtocolBranches() {
        for (HttpMethod method : new HttpMethod[] {HttpMethod.GET, HttpMethod.DELETE, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH}) {
            var plain = MockServerWebExchange.from(MockServerHttpRequest.method(method, "/demo").body("ordinary-large-body"));
            filter.filter(plain, current -> Mono.empty()).block();
            assertThat((Object)plain.getAttribute(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY)).isNull();
        }
        var get = MockServerWebExchange.from(MockServerHttpRequest.get("/demo").header("sm4-key", "encrypted")
            .header("sym-alg", "SM4").build());
        filter.filter(get, current -> Mono.empty()).block();
        assertThat((Object)get.getAttribute(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY)).isNull();
        var hmac = MockServerWebExchange.from(MockServerHttpRequest.post("/demo")
            .header("Authorization", "hmac username=\"app\", signature=\"placeholder\"").body("123"));
        filter.filter(hmac, current -> Mono.defer(() -> {
            assertThat((byte[])current.getAttribute(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY))
                .isEqualTo("123".getBytes(StandardCharsets.UTF_8));
            return Mono.empty();
        })).block();
    }

    /** 下游同类异常不能被当前聚合的 413 映射吞掉。 */
    @Test
    void preservesDownstreamError() {
        var exchange = exchange(Flux.just(buffer("1")), null);
        var error = new DataBufferLimitException("downstream");
        assertThatThrownBy(() -> filter.filter(exchange, current -> Mono.error(error)).block()).isSameAs(error);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    /** 取消、超限及上游错误释放已接收的池化缓冲。 */
    @Test
    void releasesBuffersOnCancellationOversizeAndFailure() {
        var factory = new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT);
        var first = factory.allocateBuffer();
        first.write(new byte[] {1, 2});
        var exchange = exchange(Flux.concat(Flux.just(first), Flux.never()), null);
        var subscription = filter.filter(exchange, current -> Mono.empty()).subscribe();
        subscription.dispose();
        assertThat(first.getNativeBuffer().refCnt()).isZero();
        var large = factory.allocateBuffer();
        large.write(new byte[5]);
        handleHttpErrors(exchange(Flux.just(large), null), current -> Mono.empty());
        assertThat(large.getNativeBuffer().refCnt()).isZero();
        var broken = factory.allocateBuffer();
        broken.write(new byte[] {1});
        var failure = new IllegalStateException("upstream");
        var failed = exchange(Flux.concat(Flux.just(broken), Flux.error(failure)), null);
        assertThatThrownBy(() -> filter.filter(failed, current -> Mono.empty()).block()).isSameAs(failure);
        assertThat(broken.getNativeBuffer().refCnt()).isZero();
    }
}
