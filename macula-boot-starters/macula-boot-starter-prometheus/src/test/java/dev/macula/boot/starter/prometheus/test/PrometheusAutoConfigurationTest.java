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

package dev.macula.boot.starter.prometheus.test;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * <p>
 * <b>PrometheusAutoConfigurationTest</b> Prometheus 自动配置测试
 * </p>
 * <p>
 * 测试是否正确配置application标签到所有metrics
 * </p>
 *
 * @author Rain
 * @since 2024/04/07
 */
@SpringBootTest(properties = {
        "spring.application.name=macula-prometheus-test"
})
public class PrometheusAutoConfigurationTest {

    @Autowired
    private MeterRegistryCustomizer<MeterRegistry> meterRegistryCustomizer;

    /**
     * 测试自定义器正确注入并且会添加application common tag
     */
    @Test
    void testConfigurerAddsApplicationCommonTag() {
        Assertions.assertNotNull(meterRegistryCustomizer, "MeterRegistryCustomizer should be configured");

        // 创建一个简单的registry并应用customizer
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        meterRegistryCustomizer.customize(registry);

        // 创建一个metric验证tag存在
        registry.counter("test-counter", "tag1", "value1");

        // 检查common tags配置是否包含application tag
        // 验证配置正确应用，可以通过registry配置获取
        String applicationTagValue = findCommonTagValue(registry, "application");
        Assertions.assertEquals("macula-prometheus-test", applicationTagValue,
                "Common tag 'application' should match spring.application.name");
    }

    /**
     * 查找common tag的值
     */
    private String findCommonTagValue(SimpleMeterRegistry registry, String tagKey) {
        for (io.micrometer.core.instrument.Meter meter : registry.getMeters()) {
            for (Tag tag : meter.getId().getTags()) {
                if (tag.getKey().equals(tagKey)) {
                    return tag.getValue();
                }
            }
        }
        return null;
    }
}
