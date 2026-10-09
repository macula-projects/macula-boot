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

package dev.macula.boot.starter.tinyid.config;

import dev.macula.boot.starter.tinyid.base.factory.IdGeneratorFactory;
import dev.macula.boot.starter.tinyid.base.service.SegmentIdService;
import dev.macula.boot.starter.tinyid.factory.impl.CachedIdGeneratorFactory;
import dev.macula.boot.starter.tinyid.service.impl.HttpSegmentIdServiceImpl;
import dev.macula.boot.starter.tinyid.remote.TinyIdFeignClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TinyId 分布式ID生成器自动配置类
 * 
 * @author rain
 * @since 5.0.0
 */
@AutoConfiguration
public class TinyIdAutoConfiguration {

    /**
     * 自定义号段服务存在时不注册 Feign，避免 Server 依赖客户端凭据。
     * @since 6.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnMissingBean(SegmentIdService.class)
    @EnableFeignClients(clients = TinyIdFeignClient.class)
    static class RemoteClientConfiguration {

        @Bean
        SegmentIdService segmentIdService(TinyIdFeignClient client) {
            return new HttpSegmentIdServiceImpl(client);
        }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(SegmentIdService.class)
    public IdGeneratorFactory idGeneratorFactory(SegmentIdService segmentIdService) {
        return new CachedIdGeneratorFactory(segmentIdService);
    }

}
