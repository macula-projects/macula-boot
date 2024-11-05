package dev.macula.boot.starter.cloud.alibaba.config;

import com.alibaba.cloud.nacos.registry.NacosServiceRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.ApplicationListener;

/**
 * 延迟注册服务到 Nacos
 */
public class NacosDelayRegistrationListener implements ApplicationListener<ApplicationReadyEvent> {

    private final NacosServiceRegistry nacosServiceRegistry;
    private final Registration registration;

    public NacosDelayRegistrationListener(NacosServiceRegistry nacosServiceRegistry, Registration registration) {
        this.nacosServiceRegistry = nacosServiceRegistry;
        this.registration = registration;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        nacosServiceRegistry.register(registration);
    }

}
