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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks explicit list discovery and safe traversal without granting standalone resource access.
 *
 * @author Macula
 * @since 6.1.0
 */
class PasskeyResourcesTest {

    @Test
    void findsMixedListsInsideSuccessfulResult() {
        Item item = new Item("123");
        assertThat(PasskeyResources.find(Result.success(List.of(item, "ordinary")))).containsExactly(item);
        assertThat(PasskeyResources.find(item)).isEmpty();
        assertThat(PasskeyResources.find(Result.success(item))).isEmpty();
        assertThat(PasskeyResources.find(List.of())).isEmpty();
        assertThat(PasskeyResources.find(null)).isEmpty();
    }

    @Test
    void followsPagesButNotFailedResults() throws ReflectiveOperationException {
        var constructor = PageVO.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        @SuppressWarnings("unchecked")
        PageVO<Item> page = (PageVO<Item>) constructor.newInstance();
        Item item = new Item("123");
        page.setRecords(List.of(item));
        assertThat(PasskeyResources.find(Result.success(page))).containsExactly(item);
        Result<Object> failed = Result.failed();
        failed.setData(List.of(new Item("123")));
        assertThat(PasskeyResources.find(failed)).isEmpty();
    }

    @Test
    void rejectsCyclesButAllowsRepeatedReferences() {
        List<Object> cycle = new ArrayList<>();
        cycle.add(cycle);
        assertThatThrownBy(() -> PasskeyResources.find(cycle)).isInstanceOf(ResourceAccessException.class);
        Item item = new Item("123");
        List<Item> repeated = List.of(item);
        assertThat(PasskeyResources.find(List.of(repeated, repeated))).containsExactly(item, item);
    }

    /**
     * Example list resource with a stable ID.
     * @since 6.1.0
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
