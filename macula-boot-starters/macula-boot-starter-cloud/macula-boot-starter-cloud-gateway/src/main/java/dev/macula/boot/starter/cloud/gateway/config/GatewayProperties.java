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

import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
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
     * 接口签名全局开关
     */
    private boolean signSwitch = true;

    /**
     * 后端配置了接口需要加解密时强制验证
     */
    private boolean forceCrypto = false;

    /**
     * 后端配置了接口需要签名时强制验证
     */
    private boolean forceSign = true;

    /**
     * 需要加解密的接口清单
     */
    private ProtectUrl protectUrls = new ProtectUrl();

    /**
     * 后端配置了移除OpaqueToken接口访问时强制hmac验证
     */
    private boolean forceHmacRmOpaqueTokenEndpoint = true;
    /**
     * 移除opaqueToken的端点
     */
    private String rmOpaqueTokenEndpoint = GatewayConstants.DEFAULT_RM_OPAQUE_TOKEN_ENDPOINT;

    private Gray gray = new Gray();

    @Data
    public static final class Gray {
        /** 灰度开关 */
        private boolean enabled;
    }

    @Data
    public static final class ProtectUrl {
        private List<String> crypto = new ArrayList<>();
        private List<String> sign = new ArrayList<>();
    }
}