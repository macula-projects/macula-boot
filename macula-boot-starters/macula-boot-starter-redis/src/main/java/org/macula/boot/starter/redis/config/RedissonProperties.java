package org.macula.boot.starter.redis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * <b>RedissonProperties</b> Redisson配置属性
 * </p>
 *
 * @author Rain
 * @since 2022-01-28
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "spring.redis.redisson")
public class RedissonProperties {

    private String config;

    private String file;

}
