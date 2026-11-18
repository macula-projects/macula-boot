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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 中文名 V2 脱敏规则的边界与兼容性验证。
 *
 * @author Rain
 * @since 2026/8/12
 */
class SensitiveUtilTest {

    /**
     * 验证空姓名保持返回 null 的契约。
     */
    @ParameterizedTest
    @NullAndEmptySource
    void emptyNamesReturnNull(String name) {
        assertNull(SensitiveUtil.handlerUsernameV2(name));
    }

    @ParameterizedTest
    @CsvSource({
        "张, 张",
        "张三, 张*",
        "张三丰, 张*丰",
        "欧阳娜娜, 欧*娜",
        "阿不都·热合曼, 阿*·*曼",
        "阿不都•热合曼, 阿*•*曼",
        "阿不都・热合曼, 阿*・*曼",
        "阿不都.热合曼, 阿*.*曼",
        "阿不都．热合曼, 阿*．*曼",
        "阿·依·古丽, 阿·*·*丽",
        "阿·依, 阿·*",
        "·阿依·, ·阿*·",
        "·阿依古·, ·阿*古·",
        "·张·, ·张·",
        "·•・.．, ·•・.．",
        "阿··依古, 阿··*古",
        "𠮷, 𠮷",
        "𠮷明, 𠮷*",
        "张𠮷, 张*",
        "𠮷明𠮷, 𠮷*𠮷",
        "张𠮷明, 张*明",
        "𠮷·明𠮷, 𠮷·*𠮷",
        "张三四五六, 张*六",
        "张三/欧阳娜娜, 张*/欧*娜",
        "/张三//李四/, /张*//李*/",
        "张/李四/王五六七, 张/李*/王*七",
        "阿不都·热合曼/欧阳娜娜, 阿*·*曼/欧*娜",
        "阿不都·热合曼/艾合买提·吐尔逊, 阿*·*曼/艾*·*逊",
        "𠮷明/张𠮷明/𠮷明𠮷明, 𠮷*/张*明/𠮷*明",
        "阿不都··热合曼, 阿*··*曼",
        "阿不都·热合·曼, 阿*·*·曼",
        "张三/·•・.．/李四, 张*/·•・.．/李*",
        "/, /",
        "///, ///"
    })
    /**
     * 验证姓名按字符和分隔符脱敏，覆盖 Unicode 与分段边界。
     */
    void maskNamesByCharacterCountAndPreserveSeparators(String name, String expected) {
        assertEquals(expected, SensitiveUtil.handlerUsernameV2(name));
    }

    /**
     * 验证原姓名 V1 规则不受 V2 增强影响。
     */
    @Test
    void originalUsernameRuleStillOnlyShowsFirstCharacter() {
        assertEquals("张**", SensitiveUtil.handlerUsername("张三丰"));
    }


    /**
     * 保留主线既有手机号、证件、银行卡和邮箱的回归覆盖。
     */
    @Test
    void masksCommonSensitiveValues() {
        assertThat(SensitiveUtil.handlerMobile("13812340000")).isEqualTo("138****0000");
        assertThat(SensitiveUtil.handlerIdCard("511623199001010537")).isEqualTo("511623********0537");
        assertThat(SensitiveUtil.handlerBankCard("6228481234567895579")).isEqualTo("622848*********5579");
        assertThat(SensitiveUtil.handlerEmail("guest@163.com")).isEqualTo("g****@163.com");
        assertThat(SensitiveUtil.handlerUsername("张三丰")).isEqualTo("张**");
    }

    /**
     * 保留空输入及 hide/overlay 索引越界行为的既有回归覆盖。
     */
    @Test
    void handlesEmptyAndOutOfRangeInputs() {
        assertThat(SensitiveUtil.handlerMobile(null)).isNull();
        assertThat(SensitiveUtil.hide("abc", 5, 8)).isEqualTo("abc");
        assertThat(SensitiveUtil.hide("abc", 2, 1)).isEqualTo("abc");
        assertThat(SensitiveUtil.overlay("abcdef", "*", 2, -1, 99)).isEqualTo("**");
    }
}
