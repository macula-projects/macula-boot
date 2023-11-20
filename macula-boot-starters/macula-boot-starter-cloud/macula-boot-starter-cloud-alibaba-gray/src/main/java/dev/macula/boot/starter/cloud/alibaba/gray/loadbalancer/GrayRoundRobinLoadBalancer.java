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

package dev.macula.boot.starter.cloud.alibaba.gray.loadbalancer;

import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.context.GrayVersionContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.SelectedInstanceCallback;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * {@code GrayRoundRobinLoadBalancer} 全链路灰度负载均衡
 *
 * @author rain
 * @since 2023/9/25 23:19
 */
@Slf4j
public class GrayRoundRobinLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final AtomicInteger position;
    private final String serviceId;
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    public GrayRoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
        String serviceId) {
        this(serviceInstanceListSupplierProvider, serviceId, new Random().nextInt(1000));
    }

    public GrayRoundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
        String serviceId, int seedPosition) {
        this.position = new AtomicInteger(seedPosition);
        this.serviceId = serviceId;
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier =
            serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next()
            .map(serviceInstances -> processInstanceResponse(supplier, serviceInstances, request));
    }

    private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier,
        List<ServiceInstance> serviceInstances, Request request) {
        Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances, request);
        if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {
            ((SelectedInstanceCallback)supplier).selectedServiceInstance(serviceInstanceResponse.getServer());
        }
        return serviceInstanceResponse;
    }

    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances, Request request) {
        if (instances.isEmpty()) {
            if (log.isWarnEnabled()) {
                log.warn("No servers available for service: " + serviceId);
            }
            return new EmptyResponse();
        }

        // 获取ServiceInstance列表
        instances = getInstances(instances, request);

        // Do not move position when there is only 1 instance, especially some suppliers
        // have already filtered instances
        if (instances.size() == 1) {
            return new DefaultResponse(instances.get(0));
        }

        // Ignore the sign bit, this allows pos to loop sequentially from 0 to
        // Integer.MAX_VALUE
        int pos = this.position.incrementAndGet() & Integer.MAX_VALUE;
        ServiceInstance instance = instances.get(pos % instances.size());
        return new DefaultResponse(instance);
    }

    private List<ServiceInstance> getInstances(List<ServiceInstance> serviceInstances, Request request) {

        final String grayVersion = getGrayVersion(request);
        List<ServiceInstance> instancesToChoose = serviceInstances;

        // 过滤灰度实例
        if (StringUtils.isNotBlank(grayVersion)) {
            List<ServiceInstance> grayInstances = instancesToChoose.stream().filter(instance -> {
                String grayMetaVersion = instance.getMetadata().get(GlobalConstants.GRAY_VERSION_TAG);
                return StringUtils.equals(grayMetaVersion, grayVersion);
            }).collect(Collectors.toList());

            if (!CollectionUtils.isEmpty(grayInstances)) {
                // 如果能匹配到指定GrayVersion的灰度实例，则优先选择灰度实例
                instancesToChoose = grayInstances;
            } else {
                // 没有匹配到GrayVersion灰度实例，则选择非灰度实例
                instancesToChoose = instancesToChoose.stream()
                    .filter(instance -> StrUtil.isEmpty(instance.getMetadata().get(GlobalConstants.GRAY_VERSION_TAG)))
                    .collect(Collectors.toList());
            }

            // 没有选中任何实例，退回，返回所有实例
            if (CollectionUtils.isEmpty(instancesToChoose)) {
                instancesToChoose = serviceInstances;
            }
        }
        return instancesToChoose;
    }

    private String getGrayVersion(Request<?> request) {
        if (request.getContext() instanceof RequestDataContext) {
            return ((RequestDataContext)request.getContext()).getClientRequest().getHeaders()
                .getFirst(GlobalConstants.GRAY_VERSION_TAG);
        }
        return GrayVersionContextHolder.getGrayVersion();
    }
}
