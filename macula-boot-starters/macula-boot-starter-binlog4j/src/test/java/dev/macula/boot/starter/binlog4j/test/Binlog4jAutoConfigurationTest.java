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

package dev.macula.boot.starter.binlog4j.test;

import dev.macula.boot.starter.binlog4j.BinlogClientConfig;
import dev.macula.boot.starter.binlog4j.config.Binlog4jAutoConfiguration;
import dev.macula.boot.starter.binlog4j.config.Binlog4jAutoProperties;
import dev.macula.boot.starter.binlog4j.config.Binlog4jInitializationBeanProcessor;
import dev.macula.boot.starter.binlog4j.enums.BinlogClientMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

/**
 * <p>
 * <b>Binlog4jAutoConfigurationTest</b> Binlog4j 自动配置测试
 * </p>
 * <p>
 * 测试配置属性绑定是否正确，自动配置是否正常加载
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
@SpringBootTest(properties = {
        "binlog4j.clientConfigs.main-client.host=127.0.0.1",
        "binlog4j.clientConfigs.main-client.port=3306",
        "binlog4j.clientConfigs.main-client.username=root",
        "binlog4j.clientConfigs.main-client.password=password",
        "binlog4j.clientConfigs.main-client.server-id=1",
        "binlog4j.clientConfigs.main-client.mode=standalone",
        "binlog4j.clientConfigs.second-client.host=127.0.0.1",
        "binlog4j.clientConfigs.second-client.port=3307",
        "binlog4j.clientConfigs.second-client.username=root"
})
public class Binlog4jAutoConfigurationTest {

    @Autowired
    private Binlog4jAutoProperties properties;

    @Autowired
    private Binlog4jAutoConfiguration autoConfiguration;

    @Autowired
    private Binlog4jInitializationBeanProcessor processor;

    /**
     * 测试自动配置组件是否成功注入
     */
    @Test
    void testAutoConfigurationInjected() {
        Assertions.assertNotNull(properties, "Binlog4jAutoProperties should be injected");
        Assertions.assertNotNull(autoConfiguration, "Binlog4jAutoConfiguration should be injected");
        Assertions.assertNotNull(processor, "Binlog4jInitializationBeanProcessor should be injected");
    }

    /**
     * 测试配置属性绑定是否正确，多客户端配置
     */
    @Test
    void testConfigurationPropertiesBinding() {
        Map<String, BinlogClientConfig> clientConfigs = properties.getClientConfigs();

        // 验证配置数量正确
        Assertions.assertNotNull(clientConfigs);
        Assertions.assertEquals(2, clientConfigs.size());

        // 验证第一个客户端配置
        BinlogClientConfig mainClient = clientConfigs.get("main-client");
        Assertions.assertNotNull(mainClient);
        Assertions.assertEquals("127.0.0.1", mainClient.getHost());
        Assertions.assertEquals(3306, mainClient.getPort());
        Assertions.assertEquals("root", mainClient.getUsername());
        Assertions.assertEquals("password", mainClient.getPassword());
        Assertions.assertEquals(1, mainClient.getServerId());
        Assertions.assertEquals(BinlogClientMode.standalone, mainClient.getMode());

        // 验证第二个客户端配置
        BinlogClientConfig secondClient = clientConfigs.get("second-client");
        Assertions.assertNotNull(secondClient);
        Assertions.assertEquals("127.0.0.1", secondClient.getHost());
        Assertions.assertEquals(3307, secondClient.getPort());
        Assertions.assertEquals("root", secondClient.getUsername());
    }

    /**
     * 测试 BinlogClientConfig 默认值
     */
    @Test
    void testBinlogClientConfigDefaultValues() {
        BinlogClientConfig config = new BinlogClientConfig();
        // 验证默认值（如果有的话），这里主要验证对象创建正常
        Assertions.assertNotNull(config);
    }
}
