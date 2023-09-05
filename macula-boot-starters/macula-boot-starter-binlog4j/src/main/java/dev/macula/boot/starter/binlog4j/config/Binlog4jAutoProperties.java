package dev.macula.boot.starter.binlog4j.config;

import dev.macula.boot.starter.binlog4j.BinlogClientConfig;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Data
@Configuration
@ConfigurationProperties(prefix = "binlog4j")
public class Binlog4jAutoProperties {

    private Map<String, BinlogClientConfig> clientConfigs;

}
