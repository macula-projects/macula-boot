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

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Multi-level cache implementation
 *
 * <p>Main goals are:
 *
 * <ul>
 *   <li>Bypass calls to Redis to speed up retrieval entries
 *   <li>Provide fault tolerance means if Redis is unavailable without loss in functionality
 * </ul>
 *
 * <p>WARNING: When dealing with local cache we do partial key conversion using {@link
 * RedisCache#convertKey(Object)} for consistency and retrieval of correct {@link String}
 * @author rain
 * @since 5.0.0
 */
@Slf4j
public class TwoLevelCache extends RedisCache {

    /**
     * Error messages
     */
    private static final String NO_REDIS_CONNECTION = "Redis connection factory was not found for RedisCacheWriter";
    private static final String LOCK_WAS_NOT_INITIALIZED = "Lock was not initialized";

    /**
     * These are local non-overridable properties for ReentrantLocks cache to provide atomicity
     */
    private static final long LOCKS_CACHE_MAXIMUM_SIZE = 1000;
    private static final Duration LOCKS_CACHE_EXPIRE_AFTER_ACCESS = Duration.ofSeconds(15);

    protected final TwoLevelCacheProperties properties;
    @Getter
    protected final Cache<Object, Object> localCache;
    protected final Cache<Object, ReentrantLock> locks;

    private final RedisTemplate<Object, Object> redisTemplate;
    private final Set<String> degradedOperations = ConcurrentHashMap.newKeySet();

    public TwoLevelCache(String name, TwoLevelCacheProperties properties, RedisTemplate<Object, Object> redisTemplate,
        Cache<Object, Object> localCache) {
        this(name, properties, RedisCacheWriter.nonLockingRedisCacheWriter(
                Objects.requireNonNull(redisTemplate.getConnectionFactory(), NO_REDIS_CONNECTION)), redisTemplate,
            localCache);
    }

    public TwoLevelCache(String name, TwoLevelCacheProperties properties, RedisCacheWriter redisCacheWriter,
        RedisTemplate<Object, Object> redisTemplate, Cache<Object, Object> localCache) {
        super(name, redisCacheWriter, properties.toRedisCacheConfiguration(name));

        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.localCache = localCache;
        this.locks = Caffeine.newBuilder().maximumSize(LOCKS_CACHE_MAXIMUM_SIZE)
            .expireAfterAccess(LOCKS_CACHE_EXPIRE_AFTER_ACCESS).build();
    }

    /**
     * Perform an actual lookup in the underlying store.
     *
     * <p>We do not allow storing {@code null} values, if local cache does not have mapping for
     * specified key we query Redis using fail-open error handling. If Redis contains requested mapping,
     * value will be saved in local cache. If Redis is not available, {@code null} will be returned.
     *
     * @param key the key whose associated value is to be returned
     * @return the raw store value for the key, or {@code null} if none
     */
    @Override
    @Nullable
    protected Object lookup(@NonNull Object key) {
        final String localKey = convertKey(key);
        Object localValue = localCache.getIfPresent(localKey);

        if (localValue == null) {
            Object redisValue = callRedis("read", () -> super.lookup(key));
            if (redisValue != null) {
                localCache.put(localKey, redisValue);
            }
            return redisValue;
        }

        return localValue;
    }

    /**
     * Return the value to which this cache maps the specified key, obtaining that value from {@code valueLoader} if
     * necessary. This method provides a simple substitute for the conventional "if cached, return; otherwise create,
     * cache and return" pattern.
     *
     * <p>If the {@code valueLoader} throws an exception, it is wrapped in a {@link
     * ValueRetrievalException}
     *
     * <p>If Redis cannot be queried, {@code valueLoader} will still be executed and value will be
     * stored in local cache instead.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which this cache maps the specified key
     * @throws ValueRetrievalException if the {@code valueLoader} throws an exception or retrieved value was
     *                                 {@code null}
     * @see #get(Object)
     */
    @Override
    @NonNull
    @SuppressWarnings("unchecked")
    public synchronized <T> T get(@NonNull Object key, @NonNull Callable<T> valueLoader) {
        Object result = lookup(key);
        if (result != null) {
            return (T)result;
        }

        T loadedValue;
        try {
            loadedValue = valueLoader.call();
        } catch (Exception loaderException) {
            throw new ValueRetrievalException(key, valueLoader, loaderException);
        }

        if (loadedValue == null) {
            throw new ValueRetrievalException(key, valueLoader,
                new IllegalStateException("Cache loader returned null"));
        }

        try {
            callRedis("load", () -> super.put(key, loadedValue));
        } finally {
            localCache.put(convertKey(key), loadedValue);
        }
        return loadedValue;
    }

    /**
     * Associate the specified value with the specified key in this cache.
     *
     * <p>If the cache previously contained a mapping for this key, the old value replaced by the
     * specified value.
     *
     * <p>If value is {@code null} specified key will be evicted.
     *
     * <p>Actual registration performed in an asynchronous fashion, with subsequent lookups possibly
     * not seeing the entry yet. Use {@link #putIfAbsent} for guaranteed immediate registration for current cache.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @see #putIfAbsent(Object, Object)
     */
    @Override
    public void put(@NonNull Object key, @Nullable Object value) {
        if (value == null) {
            evict(key);
            return;
        }

        boolean redisUpdated = callRedis("put", () -> super.put(key, value));
        if (redisUpdated) {
            sendViaRedis(convertKey(key));
        }

        localCache.put(convertKey(key), value);
    }

    /**
     * Atomically associate the specified value with the specified key in this cache if it is not set already.
     *
     * <p>This is equivalent to:
     *
     * <pre><code>
     * ValueWrapper existingValue = cache.get(key);
     * if (existingValue == null) {
     *     cache.put(key, value);
     * }
     * return existingValue;
     * </code></pre>
     * <p>
     * except that the action performed atomically for current cache.
     *
     * <p>If value is {@code null} specified key will be evicted.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     * @return the value to which this cache maps the specified key (which may be {@code null} itself), or also
     *     {@code null} if the cache did not contain any mapping for that key prior to this call. Returning {@code null}
     *     is therefore an indicator that the given {@code value} has been associated with the key, or it was evicted.
     * @see #put(Object, Object)
     */
    @Override
    @NonNull
    public ValueWrapper putIfAbsent(@NonNull Object key, @Nullable Object value) {
        if (value == null) {
            evict(key);
            return new SimpleValueWrapper(null);
        }

        final ReentrantLock lock = makeLock(key);
        try {
            lock.lock();
            Object existingValue = lookup(key);
            if (existingValue == null) {
                ValueWrapper redisValue = null;
                boolean redisAvailable = true;
                try {
                    redisValue = super.putIfAbsent(key, value);
                    recordRedisAvailable("putIfAbsent");
                } catch (RuntimeException failure) {
                    if (!isRedisAvailabilityFailure(failure)) {
                        throw failure;
                    }
                    redisAvailable = false;
                    recordRedisUnavailable("putIfAbsent", failure);
                }

                if (redisValue != null) {
                    localCache.put(convertKey(key), redisValue.get());
                    return redisValue;
                }
                if (redisAvailable) {
                    sendViaRedis(convertKey(key));
                }
                localCache.put(convertKey(key), value);
                return new SimpleValueWrapper(null);
            } else {
                return new SimpleValueWrapper(existingValue);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Evict the mapping for this key from this cache if it is present.
     *
     * <p>Actual eviction performed in an asynchronous fashion, with subsequent lookups possibly still
     * seeing the entry. Use {@link #evictIfPresent} for guaranteed immediate removal for current cache.
     *
     * @param key the key whose mapping is to be removed from the cache
     * @see #evictIfPresent(Object)
     */
    @Override
    public void evict(@NonNull Object key) {
        final String localKey = convertKey(key);
        try {
            if (callRedis("evict", () -> super.evict(key))) {
                sendViaRedis(localKey);
            }
        } finally {
            localCache.invalidate(localKey);
        }
    }

    @Override
    public boolean evictIfPresent(@NonNull Object key) {
        final String localKey = convertKey(key);
        final boolean localEntryPresent = localCache.getIfPresent(localKey) != null;
        try {
            Boolean redisEntryPresent = callRedis("evictIfPresent", () -> super.evictIfPresent(key));
            if (redisEntryPresent != null) {
                sendViaRedis(localKey);
            }
            return localEntryPresent || Boolean.TRUE.equals(redisEntryPresent);
        } finally {
            localCache.invalidate(localKey);
        }
    }

    /**
     * Clear the cache through removing all mappings.
     *
     * <p>Actual clearing performed in an asynchronous fashion, with subsequent lookups possibly still
     * seeing the entries. Use {@link #invalidate()} for guaranteed immediate removal of entries for current cache.
     *
     * @see #invalidate()
     */
    @Override
    public void clear() {
        try {
            if (callRedis("clear", () -> super.clear())) {
                sendViaRedis(null);
            }
        } finally {
            localCache.invalidateAll();
        }
    }

    @Override
    public boolean invalidate() {
        final boolean localEntriesPresent = localCache.estimatedSize() > 0;
        try {
            Boolean redisEntriesPresent = callRedis("invalidate", () -> super.invalidate());
            if (redisEntriesPresent != null) {
                sendViaRedis(null);
            }
            return localEntriesPresent || Boolean.TRUE.equals(redisEntriesPresent);
        } finally {
            localCache.invalidateAll();
        }
    }

    public void clearLocal(Object key) {
        log.debug("Clearing local cache entry; cache={}, clearAll={}", getName(), key == null);
        if (key == null) {
            localCache.invalidateAll();
        } else {
            localCache.invalidate(convertKey(key));
        }
    }

    private <T> T callRedis(String operation, Supplier<T> call) {
        try {
            T result = call.get();
            recordRedisAvailable(operation);
            return result;
        } catch (RuntimeException failure) {
            if (!isRedisAvailabilityFailure(failure)) {
                throw failure;
            }
            recordRedisUnavailable(operation, failure);
            return null;
        }
    }

    private boolean callRedis(String operation, Runnable call) {
        try {
            call.run();
            recordRedisAvailable(operation);
            return true;
        } catch (RuntimeException failure) {
            if (!isRedisAvailabilityFailure(failure)) {
                throw failure;
            }
            recordRedisUnavailable(operation, failure);
            return false;
        }
    }

    /**
     * @param key to send notification about eviction. Can be {@code null}.
     */
    private void sendViaRedis(@Nullable String key) {
        callRedis("publish", () -> {
            redisTemplate.convertAndSend(properties.getTopic(), new CacheEvictMessage(getName(), key));
        });
    }

    private boolean isRedisAvailabilityFailure(RuntimeException failure) {
        Throwable current = failure;
        for (int depth = 0; current != null && depth < 16; depth++) {
            if (current instanceof DataAccessResourceFailureException || current instanceof QueryTimeoutException) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return false;
    }

    private void recordRedisUnavailable(String operation, RuntimeException failure) {
        if (degradedOperations.add(operation)) {
            log.warn("Redis cache operation degraded; cache={}, operation={}, failure={}", getName(), operation,
                failure.getClass().getSimpleName());
        } else {
            log.debug("Redis cache operation remains degraded; cache={}, operation={}, failure={}", getName(),
                operation, failure.getClass().getSimpleName());
        }
    }

    private void recordRedisAvailable(String operation) {
        if (degradedOperations.remove(operation)) {
            log.info("Redis cache operation recovered; cache={}, operation={}", getName(), operation);
        }
    }

    /**
     * @param key to make lock for
     * @return new {@link ReentrantLock} for synchronizing operations
     */
    @NonNull
    private ReentrantLock makeLock(@NonNull Object key) {
        return Objects.requireNonNull(locks.get(key, o -> new ReentrantLock()), LOCK_WAS_NOT_INITIALIZED);
    }
}
