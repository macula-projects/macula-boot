/*
 * Copyright (c) 2023-2026 Macula
 * macula.dev, China
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
package dev.macula.boot.starter.tinyid;

import dev.macula.boot.starter.tinyid.base.factory.IdGeneratorFactory;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * TinyID 客户端外部服务集成测试。
 *
 * @author du_imba
 * @since 2026/8/12
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@EnabledIfEnvironmentVariable(named = "TINYID_E2E", matches = "true",
    disabledReason = "需要隔离 Gateway、TinyID Server、test 业务及 MACULA_CLOUD_APP_KEY/SECRET_KEY")
public class TinyIdClientIT {

    @Autowired
    private IdGeneratorFactory idGeneratorFactory;

    @Test
    public void testNextId() {
        var generator = idGeneratorFactory.getIdGenerator("test");
        java.util.Set<Long> ids = new java.util.HashSet<>();
        ids.add(generator.nextId());
        ids.addAll(generator.nextId(250));
        org.assertj.core.api.Assertions.assertThat(ids).hasSize(251).allMatch(id -> id > 0);
    }
}
