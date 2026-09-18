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

package dev.macula.example.tencent.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Tencent 微服务消费者示例的启动入口，开启 Polaris 服务发现和 Feign 客户端扫描。
 *
 * @author rain
 * @since 5.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class MaculaExampleTencentConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaculaExampleTencentConsumerApplication.class, args);
    }
}
