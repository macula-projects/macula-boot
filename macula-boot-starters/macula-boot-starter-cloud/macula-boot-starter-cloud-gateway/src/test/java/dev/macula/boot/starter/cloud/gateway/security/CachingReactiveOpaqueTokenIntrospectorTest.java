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


package dev.macula.boot.starter.cloud.gateway.security;

import dev.macula.boot.constants.CacheConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.resource.introspection.BadOpaqueTokenException;
import reactor.core.publisher.Mono;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

/**
 * 验证缓存命中和权威内省结果均遵守剩余有效期。
 *
 * @author Rain
 * @since 6.1.0
 */
class CachingReactiveOpaqueTokenIntrospectorTest {
    private static final Instant NOW = Instant.parse("2026-11-17T00:00:00Z");
    private static final String KEY = CacheConstants.GATEWAY_TOKEN_CACHE_KEY + "test";
    private final RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
    private final ValueOperations<String, Object> values = mock(ValueOperations.class);

    /** 装配无外部连接的缓存。 */
    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(values);
    }

    /** 创建只包含本测试所需属性的身份。 */
    private OAuth2AuthenticatedPrincipal principal(Object expiry) {
        var attributes = new HashMap<String, Object>();
        attributes.put("sub", "alice");
        if (expiry != null) {
            attributes.put("exp", expiry);
        }
        return new DefaultOAuth2AuthenticatedPrincipal("alice", attributes, List.of());
    }

    /** 创建固定时间源的缓存内省器。 */
    private CachingReactiveOpaqueTokenIntrospector introspector(OAuth2AuthenticatedPrincipal fresh) {
        return new CachingReactiveOpaqueTokenIntrospector(token -> Mono.just(fresh), redis, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** 缓存已经到期时必须重新内省，TTL 按不足一分钟的剩余时间写入。 */
    @Test
    void refreshesExpiredCacheWithRemainingTtl() {
        when(values.get(KEY)).thenReturn(principal(NOW));
        var fresh = principal(NOW.plusSeconds(11));
        assertThat(introspector(fresh).introspect("test").block().getName()).isEqualTo("alice");
        verify(redis).delete(KEY);
        verify(values).set(eq(KEY), any(OAuth2AuthenticatedPrincipal.class), eq(Duration.ofSeconds(11)));
    }

    /** 有效缓存不调用外部内省；订阅之前不执行 Redis 读取。 */
    @Test
    void usesOnlyUnexpiredCacheAndDefersIo() {
        var cached = principal(NOW.plusSeconds(2));
        when(values.get(KEY)).thenReturn(cached);
        AtomicInteger calls = new AtomicInteger();
        var introspector = new CachingReactiveOpaqueTokenIntrospector(token -> {
            calls.incrementAndGet();
            return Mono.error(new AssertionError("must not introspect"));
        }, redis, Clock.fixed(NOW, ZoneOffset.UTC));
        var result = introspector.introspect("test");
        verify(values, never()).get(KEY);
        assertThat(result.block()).isSameAs(cached);
        assertThat(calls).hasValue(0);
    }

    /** 非法时间不能作为无 exp 使用，权威内省返回过期身份也拒绝。 */
    @Test
    void rejectsMalformedOrExpiredIdentity() {
        when(values.get(KEY)).thenReturn(principal("not-an-instant"));
        assertThatThrownBy(() -> introspector(principal(NOW.plusSeconds(1))).introspect("test").block())
            .isInstanceOf(BadOpaqueTokenException.class);
        when(values.get(KEY)).thenReturn(null);
        for (Object expiry : new Object[] {NOW, NOW.minusSeconds(1), "invalid"}) {
            assertThatThrownBy(() -> introspector(principal(expiry)).introspect("test").block())
                .isInstanceOf(BadOpaqueTokenException.class);
        }
        verify(values, never()).set(any(), any(), any(Duration.class));
    }

    /** 缺少 exp 仅保留五分钟缓存，不依赖 iat 计算有效期。 */
    @Test
    void capsCacheWithoutExpiry() {
        introspector(principal(null)).introspect("test").block();
        verify(values).set(eq(KEY), any(OAuth2AuthenticatedPrincipal.class), eq(Duration.ofMinutes(5)));
    }

    /** 过期缓存不能在重新内省失败后继续使用。 */
    @Test
    void neverFallsBackAfterIntrospectionFailure() {
        when(values.get(KEY)).thenReturn(principal(NOW.minusSeconds(1)));
        var failure = new BadOpaqueTokenException("revoked");
        var introspector = new CachingReactiveOpaqueTokenIntrospector(token -> Mono.error(failure), redis,
            Clock.fixed(NOW, ZoneOffset.UTC));
        assertThatThrownBy(() -> introspector.introspect("test").block()).isSameAs(failure);
        verify(values, never()).set(any(), any(), any(Duration.class));
    }
}
