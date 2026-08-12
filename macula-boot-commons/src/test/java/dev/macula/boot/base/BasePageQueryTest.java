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
package dev.macula.boot.base;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@code BasePageQueryTest} 分页查询参数单元测试
 *
 * @author Rain
 * @since 2026/8/12
 */
class BasePageQueryTest {

    @Test
    void shouldUseDefaultPaginationValues() {
        BasePageQuery query = new BasePageQuery();

        assertThat(query.getPageNum()).isEqualTo(1);
        assertThat(query.getPageSize()).isEqualTo(10);
    }

    @Test
    void shouldAllowPaginationValuesToBeChanged() {
        BasePageQuery query = new BasePageQuery();

        query.setPageNum(3);
        query.setPageSize(50);

        assertThat(query.getPageNum()).isEqualTo(3);
        assertThat(query.getPageSize()).isEqualTo(50);
    }
}
