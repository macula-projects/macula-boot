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

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 配置加解密的独立命令行入口，不启动 Spring。
 *
 * @author Rain
 */
public final class ConfigCryptoCli {
    private ConfigCryptoCli() {
    }

    public static void main(String[] args) {
        System.exit(execute(args, System.in, System.out, System.err, System.console()));
    }

    static int execute(String[] args, InputStream input, PrintStream out, PrintStream err) {
        return execute(args, input, out, err, null);
    }

    private static int execute(String[] args, InputStream input, PrintStream out, PrintStream err, Console console) {
        try {
            if (args.length == 1 && "--help".equals(args[0])) {
                out.println("用法：java -jar <cli.jar> [encrypt|decrypt] [--key <密钥>] [--value <内容>]");
                out.println("省略操作、密钥或内容时交互输入；解密内容必须带 mpw: 前缀。");
                out.println("密钥为 16、24 或 32 个 UTF-8 字节；值等于选项名称时请通过交互输入。");
                return 0;
            }
            Options options = parse(args);
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            if (options.operation == null) {
                options.operation = read("操作（encrypt/decrypt）：", false, reader, err, console);
            }
            if (!"encrypt".equals(options.operation) && !"decrypt".equals(options.operation)) {
                throw new IllegalArgumentException("操作必须为 encrypt 或 decrypt");
            }
            if (options.key == null) {
                options.key = read("密钥：", true, reader, err, console);
            }
            if (options.value == null) {
                options.value = read("encrypt".equals(options.operation) ? "明文：" : "密文（mpw:）：",
                    "encrypt".equals(options.operation), reader, err, console);
            }
            String result = "encrypt".equals(options.operation)
                ? ConfigCryptoUtil.encrypt(options.value, options.key)
                : ConfigCryptoUtil.decrypt(options.value, options.key);
            out.println(result);
            return 0;
        } catch (IllegalArgumentException ex) {
            err.println("参数错误：" + ex.getMessage());
            return 2;
        } catch (IOException ex) {
            err.println("输入失败：无法读取完整输入");
            return 1;
        } catch (RuntimeException ex) {
            err.println("加解密失败，请检查密钥、内容及运行环境");
            return 1;
        }
    }

    private static Options parse(String[] args) {
        Options options = new Options();
        int index = 0;
        if (args.length > 0 && !args[0].startsWith("--")) {
            options.operation = args[index++];
            if (!"encrypt".equals(options.operation) && !"decrypt".equals(options.operation)) {
                throw new IllegalArgumentException("操作必须为 encrypt 或 decrypt");
            }
        }
        while (index < args.length) {
            String option = args[index++];
            if (!"--key".equals(option) && !"--value".equals(option)) {
                throw new IllegalArgumentException("存在未知或多余参数，请使用 --help");
            }
            if (index >= args.length || isOption(args[index])) {
                throw new IllegalArgumentException("选项缺少值");
            }
            String value = args[index++];
            if ("--key".equals(option)) {
                if (options.key != null) {
                    throw new IllegalArgumentException("密钥选项不能重复");
                }
                options.key = value;
            } else {
                if (options.value != null) {
                    throw new IllegalArgumentException("内容选项不能重复");
                }
                options.value = value;
            }
        }
        return options;
    }

    private static boolean isOption(String value) {
        return "--key".equals(value) || "--value".equals(value) || "--help".equals(value);
    }

    private static String read(String prompt, boolean hidden, BufferedReader reader, PrintStream err,
        Console console) throws IOException {
        err.print(prompt);
        err.flush();
        String value;
        if (console != null && hidden) {
            char[] chars = console.readPassword();
            if (chars == null) {
                throw new IOException("EOF");
            }
            try {
                value = new String(chars);
            } finally {
                Arrays.fill(chars, '\0');
            }
        } else {
            value = console == null ? reader.readLine() : console.readLine();
        }
        if (value == null) {
            throw new IOException("EOF");
        }
        return value;
    }

    private static final class Options {
        private String operation;
        private String key;
        private String value;
    }
}
