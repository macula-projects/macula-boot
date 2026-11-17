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
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.unit.DataSize;
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

    /** 仅限制进入请求体缓存分支的请求，默认 10 MiB。 */
    private DataSize maxCachedBodySize = DataSize.ofMegabytes(10);

    /** 网关内部 JWT 签名配置；默认值兼容既有部署。 */
    private Jwt jwt = new Jwt();

    /**
     * 校验缓存上限，防止以非正数意外关闭限制。
     * @param size 大于零且不超过 int 容量的字节数
     */
    public void setMaxCachedBodySize(DataSize size) {
        if (size == null || size.toBytes() <= 0 || size.toBytes() > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("请求体缓存上限必须为正数且不超过 2147483647 字节");
        }
        this.maxCachedBodySize = size;
    }

    /**
     * JWT 私钥存储；仅生成访问器，不生成含密码的 toString。
     *
     * @since 6.1.0
     */
    @Getter
    @Setter
    public static final class Jwt {
        /** file/classpath 私钥库位置，默认资源仅为兼容用途。 */
        private String keyStore = "classpath:jwk/jose.jks";
        /** KeyStore 格式，默认 JKS，也支持 PKCS12。 */
        private String keyStoreType = "JKS";
        /** 私钥库密码，默认值保留旧部署兼容，生产须覆盖。 */
        private String keyStorePassword = "felord.cn";
        /** RSA 私钥条目别名。 */
        private String keyAlias = "jose";
        /** 条目密码；未提供时使用私钥库密码。 */
        private String keyPassword;
    }

    /** 灰度开关配置。
     * @since 5.0.21
     */
    @Data
    public static final class Gray {
        /** 灰度开关 */
        private boolean enabled;
    }

    /** 加密和签名保护路径配置。
     * @since 5.0.21
     */
    @Data
    public static final class ProtectUrl {
        private List<String> crypto = new ArrayList<>();
        private List<String> sign = new ArrayList<>();
    }
}
