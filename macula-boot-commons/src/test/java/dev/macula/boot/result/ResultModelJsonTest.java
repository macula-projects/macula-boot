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
package dev.macula.boot.result;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * {@code ResultModelJsonTest} 通用响应模型JSON契约单元测试
 *
 * @author Rain
 * @since 2026/8/12
 */
class ResultModelJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldOmitEmptyOptionChildren() throws JsonProcessingException {
        Option<Integer> option = new Option<>(1, "一级选项");

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(option));

        assertThat(json.get("value").asInt()).isEqualTo(1);
        assertThat(json.get("label").asText()).isEqualTo("一级选项");
        assertThat(json.has("children")).isFalse();
    }

    @Test
    void shouldSerializeNestedOptionChildren() throws JsonProcessingException {
        Option<Integer> child = new Option<>(2, "二级选项");
        Option<Integer> parent = new Option<>(1, "一级选项", List.of(child));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(parent));

        assertThat(json.get("children")).hasSize(1);
        assertThat(json.get("children").get(0).get("value").asInt()).isEqualTo(2);
    }

    @Test
    void shouldDeserializePageResponse() throws JsonProcessingException {
        String json = """
            {"records":["A","B"],"total":12,"size":2,"current":3}
            """;

        PageVO<String> page = objectMapper.readValue(json, new TypeReference<>() {
        });

        assertThat(page.getRecords()).containsExactly("A", "B");
        assertThat(page.getTotal()).isEqualTo(12);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getCurrent()).isEqualTo(3);
    }
}
