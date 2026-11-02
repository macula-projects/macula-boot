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

package dev.macula.boot.security;

import dev.macula.boot.result.PageVO;
import dev.macula.boot.result.Result;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks explicit list discovery and safe traversal without granting standalone resource access.
 *
 * @author Rain
 * @since 5.0.21
 */
class PasskeyResourcesTest {

    @Test
    void findsMixedListsInsideSuccessfulResult() {
        Item item = new Item("123");
        assertThat(PasskeyResources.find(Result.success(Arrays.asList(item, "ordinary")))).containsExactly(item);
        assertThat(PasskeyResources.find(item)).isEmpty();
        assertThat(PasskeyResources.find(Result.success(item))).isEmpty();
        assertThat(PasskeyResources.find(Collections.emptyList())).isEmpty();
        assertThat(PasskeyResources.find(null)).isEmpty();
    }

    @Test
    void followsPagesButNotFailedResults() {
        PageVO<Item> page = new PageVO<>();
        Item item = new Item("123");
        page.setRecords(Collections.singletonList(item));
        assertThat(PasskeyResources.find(Result.success(page))).containsExactly(item);
        Result<Object> failed = Result.failed();
        failed.setData(Collections.singletonList(new Item("123")));
        assertThat(PasskeyResources.find(failed)).isEmpty();
    }

    @Test
    void rejectsCyclesAndExcessiveDepthButAllowsRepeatedReferences() {
        List<Object> cycle = new ArrayList<>();
        cycle.add(cycle);
        assertThatThrownBy(() -> PasskeyResources.find(cycle)).isInstanceOf(ResourceAccessException.class);

        Object nested = Collections.singletonList(new Item("deep"));
        for (int i = 0; i < 34; i++) {
            nested = Collections.singletonList(nested);
        }
        final Object tooDeep = nested;
        assertThatThrownBy(() -> PasskeyResources.find(tooDeep)).isInstanceOf(ResourceAccessException.class);

        Item item = new Item("123");
        List<Item> repeated = Collections.singletonList(item);
        assertThat(PasskeyResources.find(Arrays.asList(repeated, repeated))).containsExactly(item, item);
    }

    /**
     * Example list resource with a stable ID.
     * @since 5.0.21
     */
    public static class Item implements PasskeyResource {
        private final String id;
        private String passkey;

        public Item(String id) {
            this.id = id;
        }

        @Override
        public String getResourceId() {
            return id;
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
