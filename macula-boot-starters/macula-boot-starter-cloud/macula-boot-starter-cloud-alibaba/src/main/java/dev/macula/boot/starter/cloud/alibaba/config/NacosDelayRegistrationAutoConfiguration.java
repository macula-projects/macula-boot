package dev.macula.boot.starter.cloud.alibaba.config;

import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 延迟注册服务到 Nacos 配置
 */
@Configuration
@ConditionalOnBean({NacosServiceRegistry.class, Registration.class})
@ConditionalOnProperty(prefix = "spring.cloud.nacos.discovery", name = "register-enabled", havingValue = "false")
public class NacosDelayRegistrationAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "spring.cloud.nacos.discovery", name = "register-delayed", havingValue = "true")
    public NacosDelayRegistrationListener nacosDelayRegistrationListener(
            NacosServiceRegistry nacosServiceRegistry, Registration registration) {
        return new NacosDelayRegistrationListener(nacosServiceRegistry, registration);
    }

}
