package org.macula.boot.starter.cache.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import lombok.extern.slf4j.Slf4j;
import org.macula.boot.starter.cache.CacheEvictMessage;
import org.macula.boot.starter.cache.TwoLevelCache;
import org.macula.boot.starter.cache.TwoLevelCacheManager;
import org.macula.boot.starter.cache.TwoLevelCacheProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.metrics.cache.CacheMeterBinderProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Objects;

/**
 * Autoconfiguration properties for this cache
 *
 * @author Rain
 */
@Slf4j
@AutoConfiguration(before = CacheAutoConfiguration.class, after = RedisAutoConfiguration.class)
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
@EnableConfigurationProperties({
    CacheProperties.class,
    TwoLevelCacheProperties.class
})
@EnableCaching
public class TwoLevelCacheAutoConfiguration {

    public static final String CACHE_REDIS_TEMPLATE_NAME = "twoLevelCacheRedisTemplate";
    public static final String CIRCUIT_BREAKER_NAME = "twoLevelCacheCircuitBreaker";
    public static final String CIRCUIT_BREAKER_CONFIGURATION_NAME = "twoLevelCacheCircuitBreakerConfiguration";

    /**
     * Instantiates {@link RedisTemplate} to use for sending {@link org.macula.boot.starter.cache.CacheEvictMessage}
     *
     * @param connectionFactory to use in template
     * @return template to send messages about evicted entries
     */
    @Bean
    @ConditionalOnMissingBean(name = CACHE_REDIS_TEMPLATE_NAME)
    @ConditionalOnSingleCandidate(RedisConnectionFactory.class)
    public RedisTemplate<Object, Object> twoLevelCacheRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        return template;
    }

    /**
     * @param cacheProperties            for multi-level cache
     * @param twoLevelCacheRedisTemplate to send messages about evicted entries
     * @return cache manager for multi-level caching
     */
    @Bean
    public TwoLevelCacheManager cacheManager(
        ObjectProvider<CacheProperties> highLevelCacheProperties,
        TwoLevelCacheProperties cacheProperties,
        RedisTemplate<Object, Object> twoLevelCacheRedisTemplate) {
        CircuitBreaker circuitBreaker = cacheCircuitBreaker(cacheProperties);
        return new TwoLevelCacheManager(highLevelCacheProperties, cacheProperties, twoLevelCacheRedisTemplate, circuitBreaker);
    }

    /**
     * @return cache meter binder for local level of multi level cache
     */
    @Bean
    @ConditionalOnBean(TwoLevelCacheManager.class)
    @ConditionalOnClass({MeterBinder.class, CacheMeterBinderProvider.class})
    public CacheMeterBinderProvider<TwoLevelCache> twoLevelCacheCacheMeterBinderProvider() {
        return (cache, tags) -> new CaffeineCacheMetrics(cache.getLocalCache(), cache.getName(), tags);
    }

    /**
     * @param cacheProperties            for multi-level cache
     * @param twoLevelCacheRedisTemplate to receive messages about evicted entries
     * @param cacheManager               for multi-level caching
     * @return Redis topic listener to coordinate entries eviction
     */
    @Bean
    public RedisMessageListenerContainer twoLevelCacheRedisMessageListenerContainer(
        TwoLevelCacheProperties cacheProperties,
        RedisTemplate<Object, Object> twoLevelCacheRedisTemplate,
        TwoLevelCacheManager cacheManager) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(
            Objects.requireNonNull(twoLevelCacheRedisTemplate.getConnectionFactory()));
        container.addMessageListener(
            createMessageListener(twoLevelCacheRedisTemplate, cacheManager),
            new ChannelTopic(cacheProperties.getTopic()));
        return container;
    }

    /**
     * @param cacheProperties to get circuit breaker properties for fault tolerance
     * @return circuit breaker to handle Redis connection exceptions and fallback to use local cache
     */
    static CircuitBreaker cacheCircuitBreaker(
        TwoLevelCacheProperties cacheProperties) {
        CircuitBreakerRegistry cbr = CircuitBreakerRegistry.ofDefaults();

        if (!cbr.getConfiguration(CIRCUIT_BREAKER_CONFIGURATION_NAME).isPresent()) {
            TwoLevelCacheProperties.CircuitBreakerProperties props = cacheProperties.getCircuitBreaker();

            CircuitBreakerConfig.Builder cbc = CircuitBreakerConfig.custom();
            cbc.failureRateThreshold(props.getFailureRateThreshold());
            cbc.slowCallRateThreshold(props.getSlowCallRateThreshold());
            cbc.slowCallDurationThreshold(props.getSlowCallDurationThreshold());
            cbc.permittedNumberOfCallsInHalfOpenState(props.getPermittedNumberOfCallsInHalfOpenState());
            cbc.maxWaitDurationInHalfOpenState(props.getMaxWaitDurationInHalfOpenState());
            cbc.slidingWindowType(props.getSlidingWindowType());
            cbc.slidingWindowSize(props.getSlidingWindowSize());
            cbc.minimumNumberOfCalls(props.getMinimumNumberOfCalls());
            cbc.waitDurationInOpenState(props.getWaitDurationInOpenState());

            Duration recommendedMaxDurationInOpenState = cacheProperties
                .getDefaultTimeToLive()
                .multipliedBy(cacheProperties.getLocal().getExpiryJitter() - 100L)
                .dividedBy(200);

            if (props.getWaitDurationInOpenState().compareTo(recommendedMaxDurationInOpenState) <= 0) {
                log.warn(
                    "Cache circuit breaker wait duration in open state {} is more than recommended value of {}, "
                        + "this can result in local cache expiry while circuit breaker is still in OPEN state.",
                    props.getWaitDurationInOpenState(),
                    recommendedMaxDurationInOpenState);
            }

            cbr.addConfiguration(CIRCUIT_BREAKER_CONFIGURATION_NAME, cbc.build());
        }

        CircuitBreaker cb = cbr.circuitBreaker(CIRCUIT_BREAKER_NAME, CIRCUIT_BREAKER_CONFIGURATION_NAME);
        cb.getEventPublisher()
            .onError(
                event -> log.trace("Cache circuit breaker error occurred in " + event.getElapsedDuration(), event.getThrowable())
            )
            .onSlowCallRateExceeded(
                event -> log.trace("Cache circuit breaker {} calls were slow, rate exceeded", event.getSlowCallRate())
            )
            .onFailureRateExceeded(
                event -> log.trace("Cache circuit breaker {} calls failed, rate exceeded", event.getFailureRate())
            )
            .onStateTransition(
                event -> log.trace(
                        "Cache circuit breaker {} state transitioned from {} to {}",
                        event.getCircuitBreakerName(),
                        event.getStateTransition().getFromState(),
                        event.getStateTransition().getToState())
            );
        return cb;
    }

    /**
     * @param twoLevelCacheRedisTemplate to receive messages about evicted entries
     * @param cacheManager                 for multi-level caching
     * @return Redis topic message listener to coordinate entries eviction
     */
    private static MessageListener createMessageListener(RedisTemplate<Object, Object> twoLevelCacheRedisTemplate,
        TwoLevelCacheManager cacheManager) {
        return (message, pattern) -> {
            try {
                CacheEvictMessage request =
                    (CacheEvictMessage) twoLevelCacheRedisTemplate.getValueSerializer().deserialize(message.getBody());

                if (request == null) {
                    return;
                }

                String cacheName = request.getCacheName();
                String entryKey = request.getEntryKey();

                if (!StringUtils.hasText(cacheName)) {
                    return;
                }

                TwoLevelCache cache = (TwoLevelCache) cacheManager.getCache(cacheName);

                if (cache == null) {
                    return;
                }

                log.trace("Received Redis message to evict key {} from cache {}", entryKey, cacheName);
                cache.clearLocal(entryKey);

            } catch (ClassCastException e) {
                log.error(
                    "Cannot cast cache instance returned by cache manager to "
                        + TwoLevelCache.class.getName(),
                    e);
            } catch (Exception e) {
                log.debug("Unknown Redis message", e);
            }
        };
    }
}
