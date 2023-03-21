/*
 * Copyright (c) 2023 Macula
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

package dev.macula.boot.starter.crypto;

import cn.hutool.crypto.SecureUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;

/**
 * {@code TestCrypto} 加解密测试
 *
 * @author rain
 * @since 2023/3/21 18:41
 */
@SpringBootTest
public class TestCrypto {
    @Value("${test.encrypt}")
    private String plaintext;

    public static void main(String[] args) {
        String encrypted = SecureUtil.aes("abcdef1234567890".getBytes(StandardCharsets.UTF_8)).encryptBase64("hello1");
        System.out.println(encrypted);
    }

    @Test
    public void test1() {
        Assertions.assertEquals("hello1", plaintext);
    }
}
