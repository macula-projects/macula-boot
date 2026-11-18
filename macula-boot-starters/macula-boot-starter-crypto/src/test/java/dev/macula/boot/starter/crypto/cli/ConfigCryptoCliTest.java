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
package dev.macula.boot.starter.crypto.cli;

import dev.macula.boot.starter.crypto.utils.ConfigCryptoUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CLI 参数、交互和输出契约验证。
 *
 * @author Rain
 */
class ConfigCryptoCliTest {
    private static final String KEY = "abcdef1234567890";
    private static final String KNOWN = "mpw:Or0wNwIcctnekUyDfeN8cw==";

    private Result run(String input, String... args) throws Exception {
        Result result = new Result();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        result.code = ConfigCryptoCli.execute(args,
            new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)),
            new PrintStream(out, true, "UTF-8"), new PrintStream(err, true, "UTF-8"));
        result.out = out.toString("UTF-8");
        result.err = err.toString("UTF-8");
        return result;
    }

    @Test
    void parameterEncryptionAndDecryption() throws Exception {
        Result encrypted = run("", "encrypt", "--key", KEY, "--value", "hello1");
        assertEquals(0, encrypted.code);
        assertEquals(KNOWN + System.lineSeparator(), encrypted.out);
        assertEquals("", encrypted.err);
        Result decrypted = run("", "decrypt", "--value", KNOWN, "--key", KEY);
        assertEquals(0, decrypted.code);
        assertEquals("hello1" + System.lineSeparator(), decrypted.out);
        assertEquals("", decrypted.err);
    }

    @Test
    void preservesSpecialAndEmptyValues() throws Exception {
        for (String value : new String[] {"", "  中文 \t\n", "-secret", "--custom"}) {
            Result result = run("", "encrypt", "--key", KEY, "--value", value);
            assertEquals(0, result.code);
            assertEquals(ConfigCryptoUtil.encrypt(value, KEY) + System.lineSeparator(), result.out);
        }
    }

    @Test
    void fullInteractiveEncryptionAndDecryption() throws Exception {
        Result encrypted = run("encrypt\n" + KEY + "\nhello1\n");
        assertEquals(0, encrypted.code);
        assertEquals(KNOWN + System.lineSeparator(), encrypted.out);
        assertTrue(encrypted.err.contains("密钥"));
        assertFalse(encrypted.err.contains(KEY));
        Result decrypted = run("decrypt\n" + KEY + "\n" + KNOWN + "\n");
        assertEquals(0, decrypted.code);
        assertEquals("hello1" + System.lineSeparator(), decrypted.out);
    }

    @Test
    void fillsMissingFieldsInteractively() throws Exception {
        assertEquals(0, run("hello1\n", "encrypt", "--key", KEY).code);
        assertEquals(0, run(KEY + "\n", "encrypt", "--value", "hello1").code);
        assertEquals(0, run("encrypt\n", "--key", KEY, "--value", "hello1").code);
        assertEquals(ConfigCryptoUtil.encrypt("", KEY) + System.lineSeparator(),
            run("\n", "encrypt", "--key", KEY).out);
        assertEquals(ConfigCryptoUtil.encrypt("--help", KEY) + System.lineSeparator(),
            run("--help\n", "encrypt", "--key", KEY).out);
    }

    @Test
    void helpSucceedsWithoutInput() throws Exception {
        Result result = run("", "--help");
        assertEquals(0, result.code);
        assertTrue(result.out.contains("用法"));
        assertEquals("", result.err);
    }

    @Test
    void rejectsInvalidArgumentsWithoutEchoingValues() throws Exception {
        String[][] cases = {
            {"unknown"}, {"encrypt", "--unknown", "private-input"},
            {"encrypt", "--key"}, {"encrypt", "--value"},
            {"encrypt", "--key", "--value", "private-input"},
            {"encrypt", "--key", KEY, "--key", "private-input"},
            {"encrypt", "--value", "", "--value", "private-input"},
            {"encrypt", "--value", "--help"},
            {"encrypt", "--key", KEY, "--value", "private-input", "extra"},
            {"encrypt", "--key", "private-input", "--value", "private-input"}
        };
        for (String[] args : cases) {
            Result result = run("", args);
            assertEquals(2, result.code);
            assertEquals("", result.out);
            assertFalse(result.err.contains("private-input"));
            assertFalse(result.err.contains(KEY));
        }
    }

    @Test
    void eofAtEachPromptFailsWithoutOutput() throws Exception {
        for (String input : new String[] {"", "encrypt\n", "encrypt\n" + KEY + "\n"}) {
            Result result = run(input);
            assertEquals(1, result.code);
            assertEquals("", result.out);
        }
    }

    @Test
    void invalidInputAndCryptoFailureUseCorrectCodes() throws Exception {
        assertEquals(2, run("invalid\n").code);
        assertEquals(2, run("", "decrypt", "--key", KEY, "--value", "mpw:???").code);
        Result failed = run("", "decrypt", "--key", "1234567890abcdef", "--value", KNOWN);
        assertEquals(1, failed.code);
        assertEquals("", failed.out);
        assertFalse(failed.err.contains(KNOWN));
        assertFalse(failed.err.contains("1234567890abcdef"));
    }

    private static class Result {
        private int code;
        private String out;
        private String err;
    }
}
