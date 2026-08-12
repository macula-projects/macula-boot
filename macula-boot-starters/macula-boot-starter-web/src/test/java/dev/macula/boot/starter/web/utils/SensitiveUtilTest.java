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
package dev.macula.boot.starter.web.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link SensitiveUtil} 敏感信息处理测试。
 *
 * @author Rain
 * @since 2026/8/12
 */
class SensitiveUtilTest {

    @Test
    void masksCommonSensitiveValues() {
        assertThat(SensitiveUtil.handlerMobile("13812340000")).isEqualTo("138****0000");
        assertThat(SensitiveUtil.handlerIdCard("511623199001010537")).isEqualTo("511623********0537");
        assertThat(SensitiveUtil.handlerBankCard("6228481234567895579")).isEqualTo("622848*********5579");
        assertThat(SensitiveUtil.handlerEmail("guest@163.com")).isEqualTo("g****@163.com");
        assertThat(SensitiveUtil.handlerUsername("张三丰")).isEqualTo("张**");
    }

    @Test
    void handlesEmptyAndOutOfRangeInputs() {
        assertThat(SensitiveUtil.handlerMobile(null)).isNull();
        assertThat(SensitiveUtil.hide("abc", 5, 8)).isEqualTo("abc");
        assertThat(SensitiveUtil.hide("abc", 2, 1)).isEqualTo("abc");
        assertThat(SensitiveUtil.overlay("abcdef", "*", 2, -1, 99)).isEqualTo("**");
    }
}
