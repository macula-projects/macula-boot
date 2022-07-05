package org.macula.boot.starter.tinyid.config;

import org.macula.boot.starter.tinyid.base.factory.IdGeneratorFactory;
import org.macula.boot.starter.tinyid.base.service.SegmentIdService;
import org.macula.boot.starter.tinyid.factory.impl.CachedIdGeneratorFactory;
import org.macula.boot.starter.tinyid.service.impl.HttpSegmentIdServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
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
