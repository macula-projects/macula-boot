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

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds authorized list records only in explicit response wrappers, never arbitrary object graphs.
 * A single resource response is not an issuance entry point.
 *
 * @author Macula
 * @since 6.1.0
 */
public final class PasskeyResources {

    private PasskeyResources() {
    }

    /**
     * Collects resources from collections, pages and successful Result wrappers.
     * @param body actual response body (HTTP wrappers are handled by the Web layer)
     * @return list resource instances, empty for standalone resources
     * @throws ResourceAccessException for cyclic or excessively nested containers
     */
    public static List<PasskeyResource> find(Object body) {
        List<PasskeyResource> resources = new ArrayList<>();
        collect(body, false, 0, new IdentityHashMap<>(), resources);
        return resources;
    }

    private static void collect(Object value, boolean listItem, int depth, Map<Object, Boolean> path,
        List<PasskeyResource> resources) {
        if (value == null) {
            return;
        }
        if (value instanceof PasskeyResource resource) {
            if (listItem) {
                resources.add(resource);
            }
            return;
        }
        if (!(value instanceof Collection<?> || value instanceof PageVO<?> || value instanceof Result<?>)) {
            return;
        }
        if (depth > 32 || path.put(value, Boolean.TRUE) != null) {
            throw new ResourceAccessException("Invalid resource response structure");
        }
        try {
            if (value instanceof Collection<?> collection) {
                for (Object item : collection) {
                    collect(item, true, depth + 1, path, resources);
                }
            } else if (value instanceof PageVO<?> page) {
                collect(page.getRecords(), false, depth + 1, path, resources);
            } else if (value instanceof Result<?> result && result.isSuccess()) {
                collect(result.getData(), listItem, depth + 1, path, resources);
            }
        } finally {
            path.remove(value);
        }
    }
}
