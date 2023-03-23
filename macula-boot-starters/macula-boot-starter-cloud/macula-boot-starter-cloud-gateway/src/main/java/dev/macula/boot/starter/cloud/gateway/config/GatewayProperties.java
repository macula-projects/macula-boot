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

package dev.macula.boot.starter.cloud.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code GatewayProperties} 网关配置
 *
 * @author rain
 * @since 2023/3/23 16:19
 */
@ConfigurationProperties(prefix = "macula.gateway")
@Data
public class GatewayProperties {
    /**
     * 接口加解密的全局开关
     */
    private boolean cryptoSwitch = true;

    /**
     * 后端配置了接口需要加解密时强制验证
     */
    private boolean forceCrypto = false;

    /**
     * 需要加解密的接口清单
     */
    private List<String> cryptoUrls = new ArrayList<>();
}
