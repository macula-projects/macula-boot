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

package dev.macula.boot.starter.retry.config.easyretry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;

/**
 * {@code EasyRetryClientsRegistrar} Easy Retry 客户端注册器
 *
 * @author rain
 * @since 2023/7/24 16:04
 */
public class EasyRetryClientsRegistrar implements ImportBeanDefinitionRegistrar, BeanFactoryAware, EnvironmentAware {

    private StandardEnvironment standardEnvironment;
    private BeanFactory beanFactory;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        Map<String, Object> attrs = importingClassMetadata.getAnnotationAttributes(EnableEasyRetry.class.getName());
        Map<String, Object> systemEnvironment = standardEnvironment.getSystemProperties();
        assert attrs != null;
        systemEnvironment.put("easy-retry.group", attrs.get("group"));
        systemEnvironment.put("easy-retry.aop.order", attrs.get("order"));

        // 自动发现server端IP
        if (systemEnvironment.containsKey("easy-retry.server.name")) {
            LoadBalancerClient loadBalancerClient = getLoadbalancerClient();
            if (loadBalancerClient != null) {
                String serverName = (String)systemEnvironment.get("easy-retry.server.name");
                ServiceInstance instance = loadBalancerClient.choose(serverName);
                if (instance != null) {
                    systemEnvironment.put("easy-retry.server.host", instance.getHost());
                    systemEnvironment.put("easy-retry.server.port", instance.getPort());
                }
            }
        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        StandardEnvironment standardEnvironment = (StandardEnvironment)environment;
        this.standardEnvironment = standardEnvironment;
        Map<String, Object> systemEnvironment = standardEnvironment.getSystemProperties();
        systemEnvironment.put("easy-retry.enabled", true);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    private LoadBalancerClient getLoadbalancerClient() {
        return beanFactory.getBean(LoadBalancerClient.class);
    }
}
