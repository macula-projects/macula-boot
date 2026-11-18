/*
 * Copyright (c) 2026 Macula
 *   macula.dev, China
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import dev.macula.boot.starter.web.annotation.Sensitive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 手机、座机、国际格式、短号及序列化入口的脱敏契约。
 *
 * @author Rain
 * @since 6.1.0
 */
class AdaptivePhoneMaskingTest {
    @ParameterizedTest
    @CsvSource({
        "13812340000,138****0000",
        "+8613812340000,+86138****0000",
        "008613812340000,0086138****0000",
        "+86 138 1234 0000,+86 138 **** 0000",
        "+44 7400 123456,+44 740* **3456",
        "+852 51234567,+852 512*4567",
        "1,*", "12,**", "123,***", "1234,1**4", "12345,1**45",
        "123456,12**56", "1234567,12**567", "12345678,123*5678",
        "+861, +86*", "+8612, +86**", "+86123,+86***",
        "+861234,+861**4", "00861234567,008612**567",
        "0123456,01**456",
        "' 13812340000 ',' 138****0000 '"
    })
    /**
     * 验证手机国际码、短号及原格式的脱敏结果。
     */
    void mobileMasksNationalDigitsAndRetainsFormat(String input, String expected) {
        assertEquals(expected, SensitiveUtil.handlerMobile(input));
    }

    @ParameterizedTest
    @CsvSource({
        "010-87651234,010-****1234",
        "01087651234,010****1234",
        "020-87651234,020-****1234",
        "0755-87651234,0755-****1234",
        "0999-87651234,0999-****1234",
        "(010) 8765 1234,(010) **** 1234",
        "+86 10 87651234,+86 10 ****1234",
        "0086-10-87651234,0086-10-****1234",
        "+86 (010) 87651234,+86 (010) ****1234",
        "010-1,010-*", "010-12,010-**", "010-123,010-***",
        "010-1234,010-1**4", "010-12345,010-1**45",
        "010-123456,010-12**56", "010-1234567,010-12**567",
        "0755-1234567,0755-12**567",
        "+86-10-1234,+86-10-1**4",
        "0086-010-1234,0086-010-1**4",
        "+44 20 7031 3000,+44 20 **** 3000",
        "+39 02 3661 8300,+39 02 **** 8300",
        "+1 (650) 253-0000,+1 (650) 25*-*000",
        "1,*", "12,**", "123,***", "1234,1**4", "12345,1**45",
        "123456,12**56", "1234567,12**567", "12345678,****5678",
        "0000-12345678,*"
    })
    /**
     * 验证座机区号保留和短主号长度规则。
     */
    void fixedPhoneRetainsAreaAndHandlesShortSubscribers(String input, String expected) {
        assertEquals(expected, SensitiveUtil.handlerFixedPhone(input));
        assertEquals(expected, SensitiveUtil.handlerPhone(input));
    }

    /**
     * 验证明确分机号及其分隔符不参与掩码。
     */
    @ParameterizedTest
    @ValueSource(strings = {" ext 888", " ext. 888", " x888", "#888", "转888", "分机888", "-888"})
    void extensionsArePreserved(String extension) {
        String input = "010-87651234" + extension;
        String expected = "010-****1234" + extension;
        assertEquals(expected, SensitiveUtil.handlerFixedPhone(input));
        assertEquals(expected, SensitiveUtil.handlerPhone(input));
    }

    /**
     * 验证国际码和分机不会改变主号掩码范围。
     */
    @Test
    void explicitExtensionDoesNotChangeShortNumberLength() {
        assertEquals("010-1**4 ext. 99", SensitiveUtil.handlerPhone("010-1234 ext. 99"));
        assertEquals("010-*#99", SensitiveUtil.handlerFixedPhone("010-1#99"));
        assertEquals("+86-10-****1234-888", SensitiveUtil.handlerPhone("+86-10-87651234-888"));
        assertEquals("0086-10-****1234-888", SensitiveUtil.handlerPhone("0086-10-87651234-888"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"13812340000", "+86 13812340000", "008613812340000",
        "+44 7400 123456", "+852 51234567"})
    /**
     * 验证明确手机类型由自适应入口委派手机方法。
     */
    void adaptiveMobileDelegatesToMobileMethod(String input) {
        assertEquals(SensitiveUtil.handlerMobile(input), SensitiveUtil.handlerPhone(input));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "unknown 13812340000", "+999123456789", "++8613812340000",
        "13812340000 private", "(010 87651234", "010--87651234", "+", "12345678901234567890"})
    /**
     * 验证异常输入保守回退，不回显原始号码。
     */
    void unsafeInputsDoNotRevealOriginalDigits(String input) {
        if (input.isEmpty()) {
            assertEquals(null, SensitiveUtil.handlerPhone(input));
            return;
        }
        assertEquals("*", SensitiveUtil.handlerMobile(input));
        assertEquals("*", SensitiveUtil.handlerFixedPhone(input));
        assertEquals("*", SensitiveUtil.handlerPhone(input));
    }

    /**
     * 验证三个电话入口的空输入契约。
     */
    @ParameterizedTest
    @NullAndEmptySource
    void emptyInputsKeepNullContract(String input) {
        assertEquals(null, SensitiveUtil.handlerMobile(input));
        assertEquals(null, SensitiveUtil.handlerFixedPhone(input));
        assertEquals(null, SensitiveUtil.handlerPhone(input));
    }

    /**
     * 验证国际数字分组不被误认成分机。
     */
    @Test
    void originalFormattingDoesNotTurnInternationalGroupIntoExtension() {
        String masked = SensitiveUtil.handlerFixedPhone("+1-650-253-0000");
        assertEquals("+1-650-25*-*000", masked);
        assertFalse(masked.endsWith("-0000"));
    }

    /**
     * 通过真实 Jackson 序列化验证三个电话注解入口。
     */
    @Test
    void jacksonUsesAllThreeAnnotationTypes() throws Exception {
        JsonNode json = new ObjectMapper().readTree(new ObjectMapper().writeValueAsString(new PhoneView()));
        assertEquals("+86 138****0000", json.get("mobile").asText());
        assertEquals("010-****1234-888", json.get("fixed").asText());
        assertEquals("+852 512*4567", json.get("adaptive").asText());
    }

    /**
     * 验证超长输入在任何正则前回退，并保留上限以内的有效格式。
     */
    @Test
    void boundsRawInputBeforeRegexScanning() {
        String spaces = new String(new char[4096]).replace('\0', ' ');
        for (String raw : new String[] {spaces, spaces + "13812340000"}) {
            assertEquals("*", SensitiveUtil.handlerMobile(raw));
            assertEquals("*", SensitiveUtil.handlerFixedPhone(raw));
            assertEquals("*", SensitiveUtil.handlerPhone(raw));
        }
        String padding = new String(new char[245]).replace('\0', ' ');
        assertEquals(padding + "138****0000", SensitiveUtil.handlerMobile(padding + "13812340000"));
        assertEquals("*", SensitiveUtil.handlerMobile(padding + " 13812340000"));
    }

    /**
     * 用于验证三种电话注解的序列化对象。
     *
     * @since 6.1.0
     */
    static class PhoneView {
        @Sensitive(Sensitive.Type.MOBILE)
        public String mobile = "+86 13812340000";
        @Sensitive(Sensitive.Type.FIXED_PHONE)
        public String fixed = "010-87651234-888";
        @Sensitive(Sensitive.Type.PHONE)
        public String adaptive = "+852 51234567";
    }
}
