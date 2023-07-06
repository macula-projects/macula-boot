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

package dev.macula.boot.starter.task.config.xxljob;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

/**
 * xxl-job自动装配
 *
 * @author lishangbu
 * @since 2020/9/14
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(XxlJobProperties.class)
public class XxlJobConfiguration {

    /**
     * 服务名称 包含 XXL_JOB_ADMIN 则说明是 Admin
     */
    private static final String XXL_JOB_ADMIN = "xxl-job-admin";

    /**
     * 配置xxl-job 执行器，提供自动发现 xxl-job-admin 能力
     *
     * @param xxlJobProperties xxl 配置
     * @param environment      环境变量
     * @param discoveryClient  注册发现客户端
     * @return XxlJobSpringExecutor
     */
    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor(XxlJobProperties xxlJobProperties, Environment environment,
        DiscoveryClient discoveryClient) {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        XxlExecutorProperties executor = xxlJobProperties.getExecutor();
        // 应用名默认为服务名
        String appName = executor.getAppname();
        if (!StringUtils.hasText(appName)) {
            appName = environment.getProperty("spring.application.name");
        }
        String accessToken = environment.getProperty("xxl.job.accessToken");
        if (!StringUtils.hasText(accessToken)) {
            accessToken = executor.getAccessToken();
        }

        xxlJobSpringExecutor.setAppname(appName);
        xxlJobSpringExecutor.setAddress(executor.getAddress());
        xxlJobSpringExecutor.setIp(executor.getIp());
        xxlJobSpringExecutor.setPort(executor.getPort());
        xxlJobSpringExecutor.setAccessToken(accessToken);
        xxlJobSpringExecutor.setLogPath(executor.getLogPath());
        xxlJobSpringExecutor.setLogRetentionDays(executor.getLogRetentionDays());

        // 如果配置为空则获取注册中心的服务列表 "http://pig-xxl:9080/xxl-job-admin"
        final String xxlJobAdminName =
            StringUtils.hasText(xxlJobProperties.getAdmin().getName()) ? xxlJobProperties.getAdmin().getName()
                : XXL_JOB_ADMIN;

        if (!StringUtils.hasText(xxlJobProperties.getAdmin().getAddresses())) {
            String serverList = discoveryClient.getServices().stream().filter(s -> s.contains(xxlJobAdminName))
                .flatMap(s -> discoveryClient.getInstances(s).stream()).map(
                    instance -> String.format("http://%s:%s/%s", instance.getHost(), instance.getPort(),
                        xxlJobAdminName)).collect(Collectors.joining(","));
            xxlJobSpringExecutor.setAdminAddresses(serverList);
        } else {
            xxlJobSpringExecutor.setAdminAddresses(xxlJobProperties.getAdmin().getAddresses());
        }
        return xxlJobSpringExecutor;
    }
}
