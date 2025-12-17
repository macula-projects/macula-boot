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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReentrantLock;

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
    protected final CircuitBreaker cacheCircuitBreaker;

    private final RedisTemplate<Object, Object> redisTemplate;

    public TwoLevelCache(String name, TwoLevelCacheProperties properties, RedisTemplate<Object, Object> redisTemplate,
        Cache<Object, Object> localCache, CircuitBreaker cacheCircuitBreaker) {
        this(name, properties, RedisCacheWriter.nonLockingRedisCacheWriter(
                Objects.requireNonNull(redisTemplate.getConnectionFactory(), NO_REDIS_CONNECTION)), redisTemplate,
            localCache, cacheCircuitBreaker);
    }

    public TwoLevelCache(String name, TwoLevelCacheProperties properties, RedisCacheWriter redisCacheWriter,
        RedisTemplate<Object, Object> redisTemplate, Cache<Object, Object> localCache,
        CircuitBreaker cacheCircuitBreaker) {
        super(name, redisCacheWriter, properties.toRedisCacheConfiguration(name));

        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.localCache = localCache;
        this.locks = Caffeine.newBuilder().maximumSize(LOCKS_CACHE_MAXIMUM_SIZE)
            .expireAfterAccess(LOCKS_CACHE_EXPIRE_AFTER_ACCESS).build();
        this.cacheCircuitBreaker = cacheCircuitBreaker;
    }

    /**
     * Perform an actual lookup in the underlying store.
     *
     * <p>We do not allow storing {@code null} values, if local cache does not have mapping for
     * specified key we query Redis using circuit breaker and error handling logic. If Redis contains requested mapping,
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
            return callRedis(() -> super.lookup(key), value -> localCache.put(localKey, value));
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

        final String localKey = convertKey(key);
        T cacheResult = callRedis(() -> super.get(key, valueLoader), () -> {
            try {
                T value = valueLoader.call();
                localCache.put(localKey, value);
                return value;
            } catch (Exception recoverException) {
                throw new ValueRetrievalException(key, valueLoader, recoverException);
            }
        });
        
        // 保证不会返回 null，因为 fallback 逻辑总是会抛出异常或返回有效值
        if (cacheResult == null) {
            throw new ValueRetrievalException(key, valueLoader, 
                new IllegalStateException("Cache lookup returned unexpected null"));
        }
        return cacheResult;
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

        callRedis(() -> super.put(key, value));

        // 通知其他实例清除本地缓存
        sendViaRedis(convertKey(key));

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
                callRedis(() -> super.putIfAbsent(key, value));

                // 通知其他实例清除本地缓存
                sendViaRedis(convertKey(key));

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
        // 先清除redis中缓存数据，然后清除caffeine中的缓存，避免短时间内如果先清除caffeine缓存后其他请求会再从redis里加载到caffeine中
        callRedis(() -> super.evict(key));

        final String localKey = convertKey(key);

        sendViaRedis(localKey);

        localCache.invalidate(localKey);
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
        callRedis(() -> super.clear());
        sendViaRedis(null);
        localCache.invalidateAll();
    }

    public void clearLocal(Object key) {
        log.debug("clear local cache, the key is : {}", key);
        if (key == null) {
            localCache.invalidateAll();
        } else {
            localCache.invalidate(convertKey(key));
        }
    }

    /**
     * @param call to Redis
     */
    private void callRedis(@NonNull Runnable call) {
        if (properties.isOpenCircuitBreaker()) {
            cacheCircuitBreaker.executeRunnable(call);
        } else {
            call.run();
        }
    }

    /**
     * @param call to Redis
     * @param <T> return type
     * @return execution result as Supplier
     */
    private <T> java.util.function.Supplier<T> callRedis(java.util.function.Supplier<T> call) {
        return () -> {
            try {
                if (properties.isOpenCircuitBreaker()) {
                    return cacheCircuitBreaker.executeSupplier(call);
                } else {
                    return call.get();
                }
            } catch (Exception e) {
                log.debug("Redis call failed, returning null", e);
                return null;
            }
        };
    }

    /**
     * @param call to Redis with post action
     * @param postAction action to execute after successful call
     * @param <T> return type
     * @return execution result or null on failure
     */
    private <T> T callRedis(java.util.function.Supplier<T> call, java.util.function.Consumer<T> postAction) {
        try {
            T result;
            if (properties.isOpenCircuitBreaker()) {
                result = cacheCircuitBreaker.executeSupplier(call);
            } else {
                result = call.get();
            }
            if (result != null) {
                postAction.accept(result);
            }
            return result;
        } catch (Exception e) {
            log.debug("Redis call failed, returning null", e);
            return null;
        }
    }

    /**
     * @param call to Redis with fallback
     * @param fallback fallback function to execute on failure
     * @param <T> return type
     * @return execution result
     */
    private <T> T callRedis(java.util.function.Supplier<T> call, java.util.function.Supplier<T> fallback) {
        try {
            if (properties.isOpenCircuitBreaker()) {
                return cacheCircuitBreaker.executeSupplier(call);
            } else {
                return call.get();
            }
        } catch (Exception e) {
            log.debug("Redis call failed, using fallback", e);
            return fallback.get();
        }
    }

    /**
     * @param key to send notification about eviction. Can be {@code null}.
     */
    private void sendViaRedis(@Nullable String key) {
        Runnable sendMessage = () -> redisTemplate.convertAndSend(properties.getTopic(), new CacheEvictMessage(getName(), key));
        
        if (properties.isOpenCircuitBreaker()) {
            try {
                cacheCircuitBreaker.executeRunnable(sendMessage);
            } catch (Exception e) {
                log.debug("Failed to send cache eviction message via Redis", e);
            }
        } else {
            try {
                sendMessage.run();
            } catch (Exception e) {
                log.debug("Failed to send cache eviction message via Redis", e);
            }
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
