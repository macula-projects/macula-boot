/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.macula.boot.starter.websocket.config;

import dev.macula.boot.starter.websocket.stomp.RedisSimpUserRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.socket.config.annotation.DelegatingWebSocketMessageBrokerConfiguration;

import java.util.Objects;

/**
 * <p>
 * <b>MaculaWebSocketMessageBrokerConfiguration</b> 定制UserRegistry，解决集群中订阅用户相互不通的问题
 * </p>
 *
 * @author Rain
 * @since 2024/4/22
 */
@Configuration(proxyBeanMethods = false)
public class MaculaWebSocketMessageBrokerConfiguration extends DelegatingWebSocketMessageBrokerConfiguration {

    @Override
	@SuppressWarnings("unchecked")
    protected SimpUserRegistry createLocalUserRegistry(@Nullable Integer order) {
		RedisTemplate<String, Object> redisTemplate = Objects.requireNonNull(getApplicationContext()).getBean("redisTemplate", RedisTemplate.class);
		RedisSimpUserRegistry registry = new RedisSimpUserRegistry(redisTemplate);
		if (order != null) {
			registry.setOrder(order);
		}
		return registry;
    }
}
