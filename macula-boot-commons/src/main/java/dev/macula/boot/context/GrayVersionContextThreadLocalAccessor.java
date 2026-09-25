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
package dev.macula.boot.context;

import io.micrometer.context.ThreadLocalAccessor;

/**
 * {@link GrayVersionContextHolder} 的 Micrometer 上下文访问器。
 *
 * @author Rain
 * @since 6.1.0
 */
public final class GrayVersionContextThreadLocalAccessor implements ThreadLocalAccessor<String> {

    /** 灰度版本上下文在 ContextSnapshot 中使用的稳定键。 */
    public static final String KEY = "macula.gray-version";

    @Override
    public Object key() {
        return KEY;
    }

    @Override
    public String getValue() {
        return GrayVersionContextHolder.getGrayVersion();
    }

    @Override
    public void setValue(String value) {
        GrayVersionContextHolder.setGrayVersion(value);
    }

    @Override
    public void setValue() {
        GrayVersionContextHolder.clear();
    }
}
