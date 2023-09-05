package dev.macula.boot.starter.binlog4j.config;

import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;

@AutoConfiguration
@EnableConfigurationProperties(Binlog4jAutoProperties.class)
public class Binlog4jAutoConfiguration {
    @Bean
    public Binlog4jInitializationBeanProcessor binlog4jAutoInitializing(Binlog4jAutoProperties properties,
        @Nullable RedissonClient binlog4jRedissonClient) {
        return new Binlog4jInitializationBeanProcessor(properties.getClientConfigs(), binlog4jRedissonClient);
    }
}

