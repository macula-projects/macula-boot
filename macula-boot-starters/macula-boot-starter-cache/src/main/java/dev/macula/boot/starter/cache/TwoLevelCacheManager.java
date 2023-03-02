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

package dev.macula.boot.starter.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache manager to cover basic operations
 *
 * @author Rain
 */

@Slf4j
public class TwoLevelCacheManager implements CacheManager {

    private final Set<String> requestedCacheNames;
    private final TwoLevelCacheProperties properties;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final CircuitBreaker circuitBreaker;

    private final Map<String, Cache> availableCaches;

    public TwoLevelCacheManager(ObjectProvider<CacheProperties> highLevelProperties, TwoLevelCacheProperties properties,
        RedisTemplate<Object, Object> redisTemplate, CircuitBreaker circuitBreaker) {
        CacheProperties hlp = highLevelProperties.getIfAvailable();
        this.requestedCacheNames =
            hlp == null ? Collections.emptySet() : Collections.unmodifiableSet(new HashSet<>(hlp.getCacheNames()));

        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.circuitBreaker = circuitBreaker;

        this.availableCaches = new ConcurrentHashMap<>();

        this.requestedCacheNames.forEach(this::getCache);
    }

    // Workarounds for tests

    TwoLevelCacheProperties getProperties() {
        return properties;
    }

    CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
    // Workarounds for tests

    /**
     * Get or create the cache associated with the given name.
     *
     * @param name the cache identifier (must not be {@code null})
     * @return the associated cache, or {@code null} if such a cache does not exist or could be not created
     */
    @Override
    public Cache getCache(@NonNull String name) {
        if (!requestedCacheNames.isEmpty() && !requestedCacheNames.contains(name)) {
            return null;
        }

        return availableCaches.computeIfAbsent(name, key -> new TwoLevelCache(key, properties, redisTemplate,
            Caffeine.newBuilder().maximumSize(properties.getLocal().getMaxSize())
                .expireAfter(new RandomizedLocalExpiryOnWrite(key, properties)).build(), circuitBreaker));
    }

    /**
     * Get a collection of the cache names known by this manager.
     *
     * @return the names of all caches known by the cache manager
     */
    @Override
    public @NonNull Collection<String> getCacheNames() {
        return Collections.unmodifiableSet(availableCaches.keySet());
    }

    /** Expiry policy enabling randomized expiry on write for local entities */
    static class RandomizedLocalExpiryOnWrite implements Expiry<Object, Object> {

        private final Random random;
        private final Duration timeToLive;
        private final double expiryJitter;

        public RandomizedLocalExpiryOnWrite(@NonNull String name, @NonNull TwoLevelCacheProperties properties) {
            this.random = new Random(System.currentTimeMillis());
            this.timeToLive = properties.getTimeToLive().getOrDefault(name, properties.getDefaultTimeToLive());
            this.expiryJitter = properties.getLocal().getExpiryJitter();

            if (timeToLive.isNegative()) {
                throw new IllegalArgumentException("Time to live duration must be positive");
            }

            if (timeToLive.isZero()) {
                throw new IllegalArgumentException("Time to live duration must not be zero");
            }

            if (expiryJitter < 0) {
                throw new IllegalArgumentException("Expiry jitter must be positive");
            }

            if (expiryJitter >= 100) {
                throw new IllegalArgumentException("Expiry jitter must not exceed 100 percents");
            }
        }

        @Override
        public long expireAfterCreate(@NonNull Object key, @NonNull Object value, long currentTime) {
            int jitterSign = random.nextBoolean() ? 1 : -1;
            double randomJitter = 1 + (jitterSign * (expiryJitter / 100) * random.nextDouble());
            Duration expiry = timeToLive.multipliedBy((long)(100 * randomJitter)).dividedBy(200);
            log.trace("Key {} will expire in {}", key, expiry);
            return expiry.toNanos();
        }

        @Override
        public long expireAfterUpdate(@NonNull Object key, @NonNull Object value, long currentTime,
            @NonNegative long currentDuration) {
            return currentDuration;
        }

        @Override
        public long expireAfterRead(@NonNull Object key, @NonNull Object value, long currentTime,
            @NonNegative long currentDuration) {
            return currentDuration;
        }
    }
}
