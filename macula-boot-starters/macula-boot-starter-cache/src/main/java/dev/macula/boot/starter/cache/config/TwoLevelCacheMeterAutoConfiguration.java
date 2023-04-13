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

package dev.macula.boot.starter.cache.config;

import dev.macula.boot.starter.cache.TwoLevelCache;
import dev.macula.boot.starter.cache.TwoLevelCacheManager;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.boot.actuate.metrics.cache.CacheMeterBinderProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * {@code CacheMeterAutoConfiguration} 监控信息Provider
 *
 * @author rain
 * @since 2023/4/12 15:17
 */
@AutoConfiguration
@ConditionalOnClass({MeterBinder.class, CacheMeterBinderProvider.class})
@ConditionalOnBean(TwoLevelCacheManager.class)
public class TwoLevelCacheMeterAutoConfiguration {
    /**
     * @return cache meter binder for local level of multi level cache
     */
    @Bean
    public CacheMeterBinderProvider<TwoLevelCache> twoLevelCacheCacheMeterBinderProvider() {
        return (cache, tags) -> new CaffeineCacheMetrics<>(cache.getLocalCache(), cache.getName(), tags);
    }
}
