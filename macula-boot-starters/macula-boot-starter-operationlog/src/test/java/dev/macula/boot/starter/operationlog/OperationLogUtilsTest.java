/*
 * Copyright (c) 2026 Macula
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
package dev.macula.boot.starter.operationlog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

/**
 * {@link OperationLogUtils} 操作日志工具测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class OperationLogUtilsTest {

    @Test
    void serializesRegularObjectsAsJson() {
        JsonNode node = OperationLogUtils.safeToJson(Map.of("orderId", 7, "state", "PAID"));

        assertThat(node.get("orderId").asInt()).isEqualTo(7);
        assertThat(node.get("state").asText()).isEqualTo("PAID");
    }

    @Test
    void replacesInfrastructureObjectsWithSafeTypeName() {
        JsonNode node = OperationLogUtils
            .safeToJson(new MockMultipartFile("attachment", "test.txt", "text/plain", new byte[] {1}));

        assertThat(node.asText()).isEqualTo("MockMultipartFile");
    }

    @Test
    void truncatesOversizedJson() {
        JsonNode node = OperationLogUtils.safeToJson(Map.of("payload", "x".repeat(5_000)));

        assertThat(node.isTextual()).isTrue();
        assertThat(node.asText()).endsWith("...(truncated)");
    }
}
