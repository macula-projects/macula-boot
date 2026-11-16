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

package dev.macula.boot.starter.cloud.gateway.filter;

import dev.macula.boot.constants.CacheConstants;
import dev.macula.boot.starter.cloud.gateway.config.GatewayProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.redisson.Redisson;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 用两套真实连接验证System凭据序列化与共享token缓存清理，不操作业务Redis。
 *
 * @author Rain
 * @since 6.1.0
 */
@EnabledIfEnvironmentVariable(named = "IAM_CALLBACK_REDIS_IT_PORT", matches = "[0-9]+")
class RmOpaqueTokenCallbackIT {
    /** 模拟平台发布凭据、另一网关连接接收回调；只删除测试自身的随机键。 */
    @Test
    void platformCredentialAuthorizesExistingCacheRemovalAcrossConnections() throws Exception {
        var config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:" + System.getenv("IAM_CALLBACK_REDIS_IT_PORT"));
        var publisher = Redisson.create(config);
        var gateway = Redisson.create(config);
        String id = "callback-it-" + UUID.randomUUID();
        String key = CacheConstants.IAM_CALLBACK_CREDENTIAL_PREFIX + id;
        String token = "token-" + id;
        var platform = template(new RedissonConnectionFactory(publisher));
        var tokens = template(new RedissonConnectionFactory(gateway));
        var principal = new DefaultOAuth2AuthenticatedPrincipal("fixture",
                Map.of("sub", id, "jti", id), List.of());
        try {
            String apiKey = "sk-" + "a".repeat(43);
            String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(apiKey.getBytes(StandardCharsets.UTF_8)));
            var properties = new GatewayProperties();
            platform.opsForHash().putAll(key, Map.of(CacheConstants.IAM_CALLBACK_KEY_DIGEST, digest,
                    CacheConstants.IAM_CALLBACK_PATH, properties.getRmOpaqueTokenEndpoint()));
            platform.expire(key, java.time.Duration.ofSeconds(60));
            platform.opsForValue().set(CacheConstants.GATEWAY_TOKEN_CACHE_KEY + token, principal);
            platform.opsForValue().set(AddJwtGlobalFilter.buildKey(principal), "fixture-derived-jwt");
            var filter = new RmOpaqueTokenEndpointFilter(properties, tokens, platform);
            ReflectionTestUtils.setField(filter, "resourceClientId", id);
            var exchange = MockServerWebExchange.from(MockServerHttpRequest.post(properties.getRmOpaqueTokenEndpoint())
                    .header("Authorization", "Bearer " + apiKey).body("{\"token\":\"" + token + "\"}"));
            filter.filter(exchange, ignored -> Mono.error(new AssertionError("callback passed downstream"))).block();
            assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(platform.hasKey(CacheConstants.GATEWAY_TOKEN_CACHE_KEY + token)).isFalse();
            assertThat(platform.hasKey(AddJwtGlobalFilter.buildKey(principal))).isFalse();
        } finally {
            platform.delete(key);
            platform.delete(CacheConstants.GATEWAY_TOKEN_CACHE_KEY + token);
            platform.delete(AddJwtGlobalFilter.buildKey(principal));
            gateway.shutdown();
            publisher.shutdown();
        }
    }

    /** 与平台及网关当前生产模板采用相同Key/Hash Key及默认值序列化。 */
    private RedisTemplate<String, Object> template(RedissonConnectionFactory factory) {
        var template = new RedisTemplate<String, Object>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setHashKeySerializer(RedisSerializer.string());
        template.afterPropertiesSet();
        return template;
    }
}
