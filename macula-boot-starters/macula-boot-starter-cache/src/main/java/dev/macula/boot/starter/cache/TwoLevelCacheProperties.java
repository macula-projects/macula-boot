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

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple set of properties to control most aspects of the multi-level cache functionality
 *
 * @author Rain
 * @since 5.0.0
 */
@Data
@ConfigurationProperties(prefix = "spring.cache.two-level")
public class TwoLevelCacheProperties {

    /**
     * Time to live for Redis entries
     */
    private Duration defaultTimeToLive = Duration.ofHours(24L);

    /**
     * Cache specific time-to-live settings
     */
    private Map<String, Duration> timeToLive = new HashMap<>();

    /**
     * Key prefix.
     */
    private String keyPrefix = "macula:cache:";

    /**
     * Whether to use the key prefix when writing to Redis.
     */
    private boolean useKeyPrefix = true;

    /**
     * Topic to use in order to synchronize eviction of entries
     */
    private String topic = "macula:cache:two-level:topic";

    /**
     * Small subset of local cache settings
     */
    private LocalCacheProperties local = new LocalCacheProperties();

    /**
     * @return configuration for Redis cache
     */
    public RedisCacheConfiguration toRedisCacheConfiguration(String name) {
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig().disableCachingNullValues()
            .entryTtl(timeToLive.getOrDefault(name, defaultTimeToLive));

        if (useKeyPrefix) {
            configuration = configuration.prefixCacheNameWith(keyPrefix);
        }

        return configuration;
    }

    /**
     * 本地一级缓存容量与过期策略配置。
     *
     * @since 5.0.0
     */
    @Data
    public static class LocalCacheProperties {

        /**
         * Maximum amount of entities too store in local cache
         */
        private int maxSize = 2000;

        /**
         * Percent of time deviation for local cache entry expiration
         */
        private int expiryJitter = 50;
    }

}
