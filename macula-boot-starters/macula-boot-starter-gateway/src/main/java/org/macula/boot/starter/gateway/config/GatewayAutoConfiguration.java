package org.macula.boot.starter.gateway.config;

import lombok.RequiredArgsConstructor;
import org.macula.boot.starter.gateway.security.ResourceServerConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * <p>
 * <b>ResourceServerConfiguration</b> 作为资源服务的网关配置
 * </p>
 *
 * @author Rain
 * @since 2022-02-19
 */
@Configuration
@RequiredArgsConstructor
@Import(ResourceServerConfiguration.class)
public class GatewayAutoConfiguration {


}
