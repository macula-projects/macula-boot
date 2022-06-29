package org.macula.boot.starter.oauth2.resource.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * <p>
 * <b>Oauth2ResourceServerAutoConfiguration</b> Oauth2自动配置
 * </p>
 *
 * @author Rain
 * @since 2022-02-09
 */
@Configuration
@Import({ ResourceServerConfiguration.class })
public class Oauth2ResourceServerAutoConfiguration {

}
