package org.macula.boot.starter.cache;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple set of properties to control most aspects of the multi-level cache functionality
 *
 * @author Rain
 */
@Data
@ConfigurationProperties(prefix = "spring.cache.two-level")
public class TwoLevelCacheProperties {

    /** Time to live for Redis entries */
    private Duration defaultTimeToLive = Duration.ofHours(24L);

    private Map<String, Duration> timeToLive = new HashMap<>();

    /** Key prefix. */
    private String keyPrefix = "macula:cache:";

    /** Whether to use the key prefix when writing to Redis. */
    private boolean useKeyPrefix = true;

    /** Topic to use in order to synchronize eviction of entries */
    private String topic = "macula:cache:two-level:topic";

    private boolean openCircuitBreaker = true;

    /** Small subset of local cache settings */
    @NestedConfigurationProperty
    private LocalCacheProperties local = new LocalCacheProperties();

    /** Circuit breaker capability to avoid issues during Redis querying */
    @NestedConfigurationProperty
    private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();

    /** @return configuration for Redis cache */
    public RedisCacheConfiguration toRedisCacheConfiguration(String name) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(timeToLive.getOrDefault(name, defaultTimeToLive));

        if (useKeyPrefix) {
            configuration = configuration.prefixCacheNameWith(keyPrefix);
        }

        return configuration;
    }

    @Data
    public static class LocalCacheProperties {

        /** Maximum amount of entities too store in local cache */
        private int maxSize = 2000;

        /** Percent of time deviation for local cache entry expiration */
        private int expiryJitter = 50;
    }

    /**
     * Circuit breaker just records calls to Redis - it does not time out them.
     *
     * <p>To simplify defaults, we rely on 4 core properties:
     *
     * <ul>
     *   <li>{@code failureRateThreshold}
     *   <li>{@code slowCallRateThreshold}
     *   <li>{@code slowCallDurationThreshold}
     *   <li>{@code slidingWindowType}
     * </ul>
     *
     * To compute appropriate values for your properties - use your slow calls as a baseline and
     * consider sliding window type:
     *
     * <ul>
     *   <li>Assuming that call is considered to be slow after 250ms
     *   <li>Then in 1 second we should be able to process more than 4 calls
     *   <li>If sliding window is count based then {@code permittedNumberOfCallsInHalfOpenState}
     *       should be 4, {@code minimumNumberOfCalls} is 2 and {@code slidingWindowSize} is 8 (calls
     *       == 2 seconds)
     *   <li>If sliding window is time based then {@code permittedNumberOfCallsInHalfOpenState} should
     *       be 4, {@code minimumNumberOfCalls} is 2 and {@code slidingWindowSize} is 2 (seconds == 8
     *       seconds)
     * </ul>
     */
    @Data
    public static class CircuitBreakerProperties {

        /** Percent of call failures to prohibit further calls to Redis */
        private int failureRateThreshold = 25;

        /** Percent of slow calls to prohibit further calls to Redis */
        private int slowCallRateThreshold = 25;

        /** Defines the duration after which Redis call considered to be slow */
        private Duration slowCallDurationThreshold = Duration.ofMillis(250);

        /** Sliding window type for connectivity analysis */
        private CircuitBreakerConfig.SlidingWindowType slidingWindowType = CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;

        /** Amount of Redis calls to test if backend is responsive when circuit breaker closes */
        private int permittedNumberOfCallsInHalfOpenState =
            (int) (Duration.ofSeconds(5).toNanos() / slowCallDurationThreshold.toNanos());

        /** Amount of time to wait before closing circuit breaker, 0 - wait for all permitted calls. */
        private Duration maxWaitDurationInHalfOpenState =
            slowCallDurationThreshold.multipliedBy(permittedNumberOfCallsInHalfOpenState);

        /** Sliding window size for Redis calls analysis (calls / seconds) */
        private int slidingWindowSize = permittedNumberOfCallsInHalfOpenState * 2;

        /** Minimum number of calls which are required before calculating error or slow call rate */
        private int minimumNumberOfCalls = permittedNumberOfCallsInHalfOpenState / 2;

        /** Time to wait before permitting Redis calls to test backend connectivity. */
        private Duration waitDurationInOpenState =
            slowCallDurationThreshold.multipliedBy(minimumNumberOfCalls);
    }
}
