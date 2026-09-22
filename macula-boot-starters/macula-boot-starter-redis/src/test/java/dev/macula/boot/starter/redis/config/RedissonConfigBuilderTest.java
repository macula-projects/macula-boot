/*
 * Copyright (c) 2023-2026 Macula
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
package dev.macula.boot.starter.redis.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.config.Config;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.util.List;

/**
 * 验证 Spring Boot Redis 属性到 Redisson 配置的转换行为。
 *
 * @author Rain
 * @since 6.1.0
 */
public class RedissonConfigBuilderTest {

    @Test
    public void shouldBindBoot4RedisPropertyPrefix() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("spring.data.redis.host", "redis.example")
            .withProperty("spring.data.redis.port", "6380");

        DataRedisProperties properties = Binder.get(environment)
            .bind("spring.data.redis", Bindable.of(DataRedisProperties.class))
            .orElseThrow(() -> new IllegalStateException("Redis properties were not bound"));

        Assertions.assertEquals("redis.example", properties.getHost());
        Assertions.assertEquals(6380, properties.getPort());
    }

    @Test
    public void shouldUseTlsForSingleServer() throws IOException {
        DataRedisProperties properties = tlsProperties();

        Config config = RedissonConfigBuilder.create().build(null, properties, new RedissonProperties());

        Assertions.assertEquals("rediss://localhost:6379", config.useSingleServer().getAddress());
    }

    @Test
    public void shouldUseTlsForSentinelServers() throws IOException {
        DataRedisProperties properties = tlsProperties();
        DataRedisProperties.Sentinel sentinel = new DataRedisProperties.Sentinel();
        sentinel.setMaster("master");
        sentinel.setNodes(List.of("sentinel-one:26379", "redis://sentinel-two:26379"));
        properties.setSentinel(sentinel);

        Config config = RedissonConfigBuilder.create().build(null, properties, new RedissonProperties());

        Assertions.assertEquals(List.of("rediss://sentinel-one:26379", "rediss://sentinel-two:26379"),
            config.useSentinelServers().getSentinelAddresses());
    }

    @Test
    public void shouldUseTlsForClusterServers() throws IOException {
        DataRedisProperties properties = tlsProperties();
        DataRedisProperties.Cluster cluster = new DataRedisProperties.Cluster();
        cluster.setNodes(List.of("cluster-one:6379", "rediss://cluster-two:6379"));
        properties.setCluster(cluster);

        Config config = RedissonConfigBuilder.create().build(null, properties, new RedissonProperties());

        Assertions.assertEquals(List.of("rediss://cluster-one:6379", "rediss://cluster-two:6379"),
            config.useClusterServers().getNodeAddresses());
    }

    @Test
    public void shouldRejectUnsupportedSslBundle() {
        DataRedisProperties properties = tlsProperties();
        properties.getSsl().setBundle("redis-client");

        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
            () -> RedissonConfigBuilder.create().build(null, properties, new RedissonProperties()));

        Assertions.assertTrue(exception.getMessage().contains("SSL bundles"));
    }

    private DataRedisProperties tlsProperties() {
        DataRedisProperties properties = new DataRedisProperties();
        properties.getSsl().setEnabled(true);
        return properties;
    }
}
