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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.macula.boot.security.PasskeyResource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Jackson 2 response model contract used by resource credentials.
 *
 * @author Rain
 * @since 5.0.21
 */
class ResultModelJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void passkeyInterfaceHidesAuxiliaryIdButKeepsCredential() throws Exception {
        ResourceItem item = new ResourceItem();
        item.setPasskey("credential");
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(item));
        assertThat(json.has("resourceId")).isFalse();
        assertThat(json.get("passkey").asText()).isEqualTo("credential");
    }

    /**
     * Verifies inherited JSON annotations on the resource contract.
     * @since 5.0.21
     */
    public static class ResourceItem implements PasskeyResource {
        private String passkey;

        @Override
        public String getResourceId() {
            return "123";
        }

        @Override
        public String getPasskey() {
            return passkey;
        }

        @Override
        public void setPasskey(String passkey) {
            this.passkey = passkey;
        }
    }
}
