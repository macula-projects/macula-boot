/*
 * Copyright (c) 2022 Macula
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

package dev.macula.boot.starter.idempotent;

import dev.macula.boot.starter.idempotent.aspect.IdempotentAspect;
import dev.macula.boot.starter.idempotent.expression.ExpressionResolver;
import dev.macula.boot.starter.idempotent.expression.KeyResolver;
import dev.macula.boot.starter.redis.config.RedissonAutoConfiguration;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * @author lengleng
 * @date 2020/9/25
 * <p>
 * 幂等插件初始化
 */
@AutoConfiguration(after = RedissonAutoConfiguration.class)
public class IdempotentAutoConfiguration {

    /**
     * 切面 拦截处理所有 @Idempotent
     *
     * @return Aspect
     */
    @Bean
    public IdempotentAspect idempotentAspect(RedissonClient redisson, KeyResolver keyResolver) {
        return new IdempotentAspect(redisson, keyResolver);
    }

    /**
     * key 解析器
     *
     * @return KeyResolver
     */
    @Bean
    @ConditionalOnMissingBean(KeyResolver.class)
    public KeyResolver keyResolver() {
        return new ExpressionResolver();
    }

}
