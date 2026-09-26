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
package dev.macula.boot.starter.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache.ValueRetrievalException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * {@link TwoLevelCache} 的 Redis 故障降级单元测试。
 *
 * @author Rain
 * @since 6.1.0
 */
class TwoLevelCacheTest {

    private static final String CACHE_NAME = "users";
    private static final String KEY = "user-1";

    private RedisCacheWriter redisCacheWriter;
    private RedisTemplate<Object, Object> redisTemplate;
    private Cache<Object, Object> localCache;
    private TwoLevelCache cache;

    @BeforeEach
    void setUp() {
        redisCacheWriter = mock(RedisCacheWriter.class);
        redisTemplate = mock(RedisTemplate.class);
        localCache = Caffeine.newBuilder().build();
        cache = new TwoLevelCache(CACHE_NAME, new TwoLevelCacheProperties(), redisCacheWriter, redisTemplate,
            localCache);
    }

    @Test
    void localHitDoesNotAccessRedis() {
        localCache.put(KEY, "local-value");

        assertThat(cache.get(KEY, String.class)).isEqualTo("local-value");

        verify(redisCacheWriter, never()).get(anyString(), any());
    }

    @Test
    void redisHitIsStoredInLocalCache() {
        when(redisCacheWriter.get(anyString(), any())).thenReturn(RedisSerializer.java().serialize("redis-value"));

        assertThat(cache.get(KEY, String.class)).isEqualTo("redis-value");
        assertThat(localCache.getIfPresent(KEY)).isEqualTo("redis-value");
    }

    @Test
    void redisConnectionFailureInvokesLoaderOnceAndCachesLocally() {
        when(redisCacheWriter.get(anyString(), any())).thenThrow(new DataAccessResourceFailureException("down"));
        doThrow(new DataAccessResourceFailureException("down"))
            .when(redisCacheWriter).put(anyString(), any(), any(), any());
        AtomicInteger invocations = new AtomicInteger();

        String value = cache.get(KEY, () -> {
            invocations.incrementAndGet();
            return "loaded-value";
        });

        assertThat(value).isEqualTo("loaded-value");
        assertThat(invocations).hasValue(1);
        assertThat(localCache.getIfPresent(KEY)).isEqualTo("loaded-value");
    }

    @Test
    void loaderFailureIsNotRetried() {
        when(redisCacheWriter.get(anyString(), any())).thenReturn(null);
        AtomicInteger invocations = new AtomicInteger();

        assertThatThrownBy(() -> cache.get(KEY, () -> {
            invocations.incrementAndGet();
            throw new IllegalStateException("loader failed");
        })).isInstanceOf(ValueRetrievalException.class)
            .hasRootCauseMessage("loader failed");

        assertThat(invocations).hasValue(1);
    }

    @Test
    void loadedValueIsCachedLocallyWhenRedisWriteFailureIsPropagated() {
        when(redisCacheWriter.get(anyString(), any())).thenReturn(null);
        doThrow(new IllegalStateException("broken serializer"))
            .when(redisCacheWriter).put(anyString(), any(), any(), any());
        AtomicInteger invocations = new AtomicInteger();

        assertThatThrownBy(() -> cache.get(KEY, () -> {
            invocations.incrementAndGet();
            return "loaded-value";
        })).isInstanceOf(IllegalStateException.class)
            .hasMessage("broken serializer");

        assertThat(invocations).hasValue(1);
        assertThat(localCache.getIfPresent(KEY)).isEqualTo("loaded-value");
    }

    @Test
    void unknownRedisFailureIsPropagated() {
        when(redisCacheWriter.get(anyString(), any())).thenThrow(new IllegalStateException("broken serializer"));

        assertThatThrownBy(() -> cache.get(KEY)).isInstanceOf(IllegalStateException.class)
            .hasMessage("broken serializer");
    }

    @Test
    void availabilityFailureInCauseChainIsDegraded() {
        when(redisCacheWriter.get(anyString(), any()))
            .thenThrow(new IllegalStateException("wrapped", new QueryTimeoutException("timeout")));

        assertThat(cache.get(KEY)).isNull();
    }

    @Test
    void putUpdatesLocalCacheWhenRedisIsUnavailable() {
        doThrow(new DataAccessResourceFailureException("down"))
            .when(redisCacheWriter).put(anyString(), any(), any(), any());

        cache.put(KEY, "new-value");

        assertThat(localCache.getIfPresent(KEY)).isEqualTo("new-value");
        verify(redisTemplate, never()).convertAndSend(anyString(), any());
    }

    @Test
    void putIfAbsentUpdatesLocalCacheWhenRedisTimesOut() {
        when(redisCacheWriter.get(anyString(), any())).thenReturn(null);
        when(redisCacheWriter.putIfAbsent(anyString(), any(), any(), any()))
            .thenThrow(new QueryTimeoutException("timeout"));

        assertThat(cache.putIfAbsent(KEY, "new-value")).extracting(org.springframework.cache.Cache.ValueWrapper::get)
            .isNull();
        assertThat(localCache.getIfPresent(KEY)).isEqualTo("new-value");
    }

    @Test
    void evictAndClearAlwaysInvalidateLocalCacheOnRedisAvailabilityFailure() {
        localCache.put(KEY, "old-value");
        localCache.put("user-2", "old-value-2");
        doThrow(new DataAccessResourceFailureException("down"))
            .when(redisCacheWriter).evict(anyString(), any());

        cache.evict(KEY);

        assertThat(localCache.getIfPresent(KEY)).isNull();
        doThrow(new QueryTimeoutException("timeout"))
            .when(redisCacheWriter).clean(anyString(), any());

        cache.clear();

        assertThat(localCache.estimatedSize()).isZero();
    }

    @Test
    void immediateEvictionOperationsAlsoInvalidateLocalCache() {
        localCache.put(KEY, "old-value");
        when(redisCacheWriter.evictIfPresent(anyString(), any())).thenReturn(true);

        assertThat(cache.evictIfPresent(KEY)).isTrue();
        assertThat(localCache.getIfPresent(KEY)).isNull();

        localCache.put(KEY, "old-value");
        when(redisCacheWriter.invalidate(anyString(), any())).thenReturn(true);

        assertThat(cache.invalidate()).isTrue();
        assertThat(localCache.estimatedSize()).isZero();
    }

    @Test
    void immediateEvictionPublishesWhenRedisOperationFindsNoEntry() {
        when(redisCacheWriter.evictIfPresent(anyString(), any())).thenReturn(false);

        assertThat(cache.evictIfPresent(KEY)).isFalse();

        verify(redisTemplate).convertAndSend(anyString(), any());
    }

    @Test
    void immediateInvalidationPublishesWhenRedisOperationFindsNoEntries() {
        when(redisCacheWriter.invalidate(anyString(), any())).thenReturn(false);

        assertThat(cache.invalidate()).isFalse();

        verify(redisTemplate).convertAndSend(anyString(), any());
    }

    @Test
    void publishFailureDoesNotChangeSuccessfulPut() {
        doNothing().when(redisCacheWriter).put(anyString(), any(), any(), any());
        when(redisTemplate.convertAndSend(anyString(), any()))
            .thenThrow(new DataAccessResourceFailureException("down"));

        cache.put(KEY, "new-value");

        assertThat(localCache.getIfPresent(KEY)).isEqualTo("new-value");
    }
}
