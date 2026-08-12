/*
 * Copyright (c) 2026 Macula
 * macula.dev, China
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
package dev.macula.boot.starter.cloud.alibaba.gray.loadbalancer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.GrayVersionContextHolder;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import reactor.core.publisher.Flux;

/**
 * {@link GrayRoundRobinLoadBalancer} 灰度轮询负载均衡测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class GrayRoundRobinLoadBalancerTest {

    @AfterEach
    void clearGrayContext() {
        GrayVersionContextHolder.clear();
    }

    @Test
    void selectsMatchingGrayInstance() {
        ServiceInstance stable = instance("stable", Map.of());
        ServiceInstance gray = instance("gray", Map.of(GlobalConstants.GRAY_VERSION_TAG, "v2"));
        GrayVersionContextHolder.setGrayVersion("v2");

        Response<ServiceInstance> response = loadBalancer(List.of(stable, gray)).choose(new DefaultRequest<>()).block();

        assertThat(response).isNotNull();
        assertThat(response.getServer().getInstanceId()).isEqualTo("gray");
    }

    @Test
    void selectsStableInstanceWhenNoGrayVersionIsRequested() {
        ServiceInstance stable = instance("stable", Map.of());
        ServiceInstance gray = instance("gray", Map.of(GlobalConstants.GRAY_VERSION_TAG, "v2"));

        Response<ServiceInstance> response = loadBalancer(List.of(stable, gray)).choose(new DefaultRequest<>()).block();

        assertThat(response).isNotNull();
        assertThat(response.getServer().getInstanceId()).isEqualTo("stable");
    }

    private GrayRoundRobinLoadBalancer loadBalancer(List<ServiceInstance> instances) {
        ServiceInstanceListSupplier supplier = new ServiceInstanceListSupplier() {
            @Override
            public String getServiceId() {
                return "orders";
            }

            @Override
            public Flux<List<ServiceInstance>> get() {
                return Flux.just(instances);
            }
        };
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("supplier", supplier);
        ObjectProvider<ServiceInstanceListSupplier> provider = beanFactory
            .getBeanProvider(ServiceInstanceListSupplier.class);
        return new GrayRoundRobinLoadBalancer(provider, "orders", 0);
    }

    private ServiceInstance instance(String id, Map<String, String> metadata) {
        ServiceInstance instance = mock(ServiceInstance.class);
        when(instance.getInstanceId()).thenReturn(id);
        when(instance.getMetadata()).thenReturn(metadata);
        return instance;
    }
}
