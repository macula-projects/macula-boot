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

package dev.macula.example.gateway;

import dev.macula.boot.starter.cloud.gateway.crypto.CryptoService;
import dev.macula.example.gateway.crypto.CryptoLocaleServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;

/**
 * {@code MaculaExampleGatewayApplication} 网关应用
 *
 * @author rain
 * @since 2022/7/23 14:37
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MaculaExampleGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaculaExampleGatewayApplication.class, args);
    }
}
