/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;

import java.util.List;

/**
 * <p>
 * <b>GatewayPropertiesTest</b> 网关配置属性测试
 * </p>
 * <p>
 * 测试配置属性绑定是否正确，默认值是否正确
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
@SpringBootTest(properties = {
        "macula.gateway.crypto-switch=false",
        "macula.gateway.sign-switch=true",
        "macula.gateway.force-crypto=true",
        "macula.gateway.protect-urls.crypto[0]=/api/crypto/**",
        "macula.gateway.protect-urls.crypto[1]=/api/encrypt/**",
        "macula.gateway.protect-urls.sign[0]=/api/sign/**",
        "macula.gateway.gray.enabled=true"
})
@TestConfiguration
public class GatewayPropertiesTest {

    @Autowired
    private GatewayProperties gatewayProperties;

    /**
     * 测试默认值
     */
    @Test
    void testDefaultValues() {
        GatewayProperties properties = new GatewayProperties();
        Assertions.assertTrue(properties.isCryptoSwitch(), "Default cryptoSwitch should be true");
        Assertions.assertTrue(properties.isSignSwitch(), "Default signSwitch should be true");
        Assertions.assertFalse(properties.isForceCrypto(), "Default forceCrypto should be false");
        Assertions.assertTrue(properties.isForceSign(), "Default forceSign should be true");
        Assertions.assertNotNull(properties.getProtectUrls(), "Default protectUrls should be initialized");
        Assertions.assertTrue(properties.getProtectUrls().getCrypto().isEmpty(), "Default crypto list should be empty");
        Assertions.assertTrue(properties.getProtectUrls().getSign().isEmpty(), "Default sign list should be empty");
        Assertions.assertNotNull(properties.getGray(), "Default gray should be initialized");
        Assertions.assertFalse(properties.getGray().isEnabled(), "Default gray enabled should be false");
        Assertions.assertEquals("/gateway/rm/opaqueToken", properties.getRmOpaqueTokenEndpoint(),
                "Default rmOpaqueTokenEndpoint should match constant");
    }

    /**
     * 测试配置属性绑定
     */
    @Test
    void testConfigurationBinding() {
        // 验证自定义配置覆盖了默认值
        Assertions.assertFalse(gatewayProperties.isCryptoSwitch());
        Assertions.assertTrue(gatewayProperties.isSignSwitch());
        Assertions.assertTrue(gatewayProperties.isForceCrypto());

        // 验证列表配置绑定正确
        GatewayProperties.ProtectUrl protectUrls = gatewayProperties.getProtectUrls();
        List<String> cryptoList = protectUrls.getCrypto();
        Assertions.assertEquals(2, cryptoList.size());
        Assertions.assertEquals("/api/crypto/**", cryptoList.get(0));
        Assertions.assertEquals("/api/encrypt/**", cryptoList.get(1));

        List<String> signList = protectUrls.getSign();
        Assertions.assertEquals(1, signList.size());
        Assertions.assertEquals("/api/sign/**", signList.get(0));

        // 验证嵌套配置正确
        GatewayProperties.Gray gray = gatewayProperties.getGray();
        Assertions.assertTrue(gray.isEnabled());
    }
}
