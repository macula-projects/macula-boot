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

package dev.macula.boot.starter.websocket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>
 * <b>WebSocketProperties</b> Websocket配置
 * </p>
 *
 * @author Rain
 * @since 2024/4/20
 */
@ConfigurationProperties(prefix = "macula.websocket")
@Data
public class WebSocketProperties {
    /**
     * 是否开启websocket自动配置
     */
    private boolean enabled = true;

    /**
     * 是否允许测试白名单
     */
    private boolean permitTest = true;

    /**
     * endpoint端点
     */
    private String[] endpoint = new String[]{"/websocket"};

    /**
     * broker handler的处理前缀
     */
    private String[] brokerDestinationPrefixes = new String[]{"/topic", "/queue"};

    /**
     * SimpAnnotationMethodMessageHandler的处理前缀，客户端请求要加上
     */
    private String[] appDestinationPrefixes = new String[]{"/app"};

    /**
     * UserDestinationMessageHandler的处理前缀
     */
    private String userDestinationPrefix = "/user";

    private long[] heartbeat = new long[]{10000, 10000};

}
