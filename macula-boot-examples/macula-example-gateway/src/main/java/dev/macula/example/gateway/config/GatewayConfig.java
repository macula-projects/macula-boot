/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.macula.example.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.HttpHandler;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * {@code GateWayConfig} SpringBoot配置SSL同时支持http和https访问实现
 *
 * @author Rain
 * @since 2024/12/19 08:38
 */


@Configuration
public class GatewayConfig {

    private final HttpHandler httpHandler;

    @Value("${server.http.port}")
    private int httpPort;

    private WebServer webServer;

    public GatewayConfig(HttpHandler httpHandler) {
        this.httpHandler = httpHandler;
    }

    @PostConstruct
    public void start() {
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory(httpPort);
        webServer = factory.getWebServer(httpHandler);
        webServer.start();
    }

    @PreDestroy
    public void stop() {
        webServer.stop();
    }

}

