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
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * TinyId 分布式ID生成器自动配置类
 * 
 * @author rain
 */
@AutoConfiguration
@EnableConfigurationProperties(TinyIdProperties.class)
public class TinyIdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SegmentIdService segmentIdService(TinyIdProperties properties) {
        return new HttpSegmentIdServiceImpl(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public IdGeneratorFactory idGeneratorFactory(SegmentIdService segmentIdService) {
        return new CachedIdGeneratorFactory(segmentIdService);
    }

}
