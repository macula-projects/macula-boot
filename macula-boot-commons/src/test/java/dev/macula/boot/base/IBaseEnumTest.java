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
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import dev.macula.boot.enums.DataScopeEnum;
import dev.macula.boot.enums.GenderEnum;
import dev.macula.boot.enums.StatusEnum;
import org.junit.jupiter.api.Test;

/**
 * {@code IBaseEnumTest} 通用枚举转换单元测试
 *
 * @author Rain
 * @since 2026/8/12
 */
class IBaseEnumTest {

    @Test
    void shouldFindEnumByValue() {
        assertThat(IBaseEnum.getEnumByValue(1, GenderEnum.class)).isEqualTo(GenderEnum.MALE);
        assertThat(IBaseEnum.getEnumByValue(9, DataScopeEnum.class)).isEqualTo(DataScopeEnum.DEFAULT);
    }

    @Test
    void shouldReturnNullWhenValueDoesNotMatch() {
        assertThat(IBaseEnum.getEnumByValue(99, GenderEnum.class)).isNull();
        assertThat(IBaseEnum.getLabelByValue(99, GenderEnum.class)).isNull();
    }

    @Test
    void shouldConvertBetweenValueAndLabel() {
        assertThat(IBaseEnum.getLabelByValue(1, StatusEnum.class)).isEqualTo("启用");
        assertThat(IBaseEnum.getValueByLabel("禁用", StatusEnum.class)).isEqualTo(0);
        assertThat(IBaseEnum.getValueByLabel("不存在", StatusEnum.class)).isNull();
    }

    @Test
    void shouldRejectNullLookupKeys() {
        assertThatNullPointerException().isThrownBy(() -> IBaseEnum.getEnumByValue(null, GenderEnum.class));
        assertThatNullPointerException().isThrownBy(() -> IBaseEnum.getLabelByValue(null, GenderEnum.class));
        assertThatNullPointerException().isThrownBy(() -> IBaseEnum.getValueByLabel(null, GenderEnum.class));
    }
}
