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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 中文名 V2 脱敏规则的边界与兼容性验证。
 *
 * @author Rain
 */
class SensitiveUtilTest {

    @ParameterizedTest
    @NullAndEmptySource
    void emptyNamesReturnNull(String name) {
        assertNull(SensitiveUtil.handlerUsernameV2(name));
    }

    @ParameterizedTest
    @CsvSource({
        "张, 张",
        "张三, *三",
        "李四, *四",
        "张伟, *伟",
        "阿•依, *•依",
        "阿・依, *・依",
        "阿.依, *.依",
        "阿．依, *．依",
        "张三丰, 张*丰",
        "欧阳娜娜, 欧*娜",
        "阿不都·热合曼, 阿*·*曼",
        "阿不都•热合曼, 阿*•*曼",
        "阿不都・热合曼, 阿*・*曼",
        "阿不都.热合曼, 阿*.*曼",
        "阿不都．热合曼, 阿*．*曼",
        "阿·依·古丽, 阿·*·*丽",
        "阿·依, *·依",
        "·阿依·, ·*依·",
        "·阿依古·, ·阿*古·",
        "·张·, ·张·",
        "·•・.．, ·•・.．",
        "阿··依古, 阿··*古",
        "𠮷, 𠮷",
        "𠮷明, *明",
        "张𠮷, *𠮷",
        "𠮷明𠮷, 𠮷*𠮷",
        "张𠮷明, 张*明",
        "𠮷·明𠮷, 𠮷·*𠮷",
        "张三四五六, 张*六",
        "张三/欧阳娜娜, *三/欧*娜",
        "/张三//李四/, /*三//*四/",
        "张/李四/王五六七, 张/*四/王*七",
        "阿不都·热合曼/欧阳娜娜, 阿*·*曼/欧*娜",
        "阿不都·热合曼/艾合买提·吐尔逊, 阿*·*曼/艾*·*逊",
        "𠮷明/张𠮷明/𠮷明𠮷明, *明/张*明/𠮷*明",
        "阿不都··热合曼, 阿*··*曼",
        "阿不都·热合·曼, 阿*·*·曼",
        "张三/·•・.．/李四, *三/·•・.．/*四",
        "/, /",
        "///, ///"
    })
    void maskNamesByCharacterCountAndPreserveSeparators(String name, String expected) {
        assertEquals(expected, SensitiveUtil.handlerUsernameV2(name));
    }

    @Test
    void originalUsernameRuleStillOnlyShowsFirstCharacter() {
        assertEquals("张**", SensitiveUtil.handlerUsername("张三丰"));
    }
}
