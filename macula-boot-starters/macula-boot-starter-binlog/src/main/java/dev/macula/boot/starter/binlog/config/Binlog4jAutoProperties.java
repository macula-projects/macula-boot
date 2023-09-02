package dev.macula.boot.starter.binlog.config;

import dev.macula.boot.starter.binlog.BinlogClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "spring.binlog4j")
public class Binlog4jAutoProperties {

    private Map<String, BinlogClientConfig> clientConfigs;

    private RedisConfig redisConfig;

    public Map<String, BinlogClientConfig> getClientConfigs() {
        return clientConfigs;
    }

    public void setClientConfigs(Map<String, BinlogClientConfig> clientConfigs) {
        this.clientConfigs = clientConfigs;
    }

    public RedisConfig getRedisConfig() {
        return redisConfig;
    }

    public void setRedisConfig(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

}
