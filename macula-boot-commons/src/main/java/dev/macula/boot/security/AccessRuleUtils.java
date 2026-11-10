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

package dev.macula.boot.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import org.bouncycastle.util.IPAddress;

/**
 * 规范化应用访问规则，以纯字面量解析 IP/CIDR，不触发 DNS 或读取请求身份。
 *
 * @author Rain
 * @since 6.1.0
 */
public final class AccessRuleUtils {
    /** 单个 URL/IP 配置的最大字符数，与管理 API 和数据库一致。 */
    public static final int MAX_RULES_LENGTH = 2000;

    /** 禁止实例化无状态规则工具。 */
    private AccessRuleUtils() {
    }

    /**
     * 将历史逗号或换行分隔的 URL 转为 LF；空白表示不限，纯分隔符拒绝。
     *
     * @param value 原始 URL 规则
     * @return 规范化多行规则，未配置时为空字符串
     * @throws IllegalArgumentException 超长或非空但无有效条目
     */
    public static String normalizeUrls(String value) {
        return String.join("\n", splitRules(value, "[,\\r\\n]"));
    }

    /**
     * 拆分 URL 规则并匹配请求目标；由调用方提供路径匹配语义，公共模块不依赖网关框架。
     *
     * @param value URL 规则，兼容逗号与换行；空白表示不限
     * @param path 请求目标，例如 HTTP方法:/路径；非空配置下缺失即不匹配
     * @param matcher 匹配函数，参数依次为规则和请求目标
     * @return 任一规则匹配；未配置时返回 true
     * @throws IllegalArgumentException 超长或非空但无有效条目，调用方必须按拒绝处理
     */
    public static boolean matchesUrl(String value, String path, BiPredicate<String, String> matcher) {
        List<String> rules = splitRules(value, "[,\\r\\n]");
        return rules.isEmpty() || (path != null && rules.stream().anyMatch(rule -> matcher.test(rule, path)));
    }

    /**
     * 校验每条 IP/CIDR 后按 LF 保存；保留字面量拼写，不接受主机名或端口。
     *
     * @param value 每行一条的 IP/CIDR
     * @return 规范化多行配置，未配置时为空字符串
     * @throws IllegalArgumentException 任何条目非法或超长
     */
    public static String normalizeIps(String value) {
        List<String> rules = splitRules(value, "[\\r\\n]");
        rules.forEach(AccessRuleUtils::parseNetwork);
        return String.join("\n", rules);
    }

    /**
     * 完整校验配置后再匹配地址，不能让前面的命中掩盖后面的损坏条目。
     *
     * @param value 每行一条的 IP/CIDR；空白表示不限
     * @param address 直连对端的原始地址字节；非空配置下缺失即不匹配
     * @return 任一有效规则匹配；未配置时返回 true
     * @throws IllegalArgumentException 规则非法，调用方必须按拒绝处理
     */
    public static boolean matchesIp(String value, byte[] address) {
        List<Network> networks = splitRules(value, "[\\r\\n]").stream()
            .map(AccessRuleUtils::parseNetwork).toList();
        if (networks.isEmpty()) {
            return true;
        }
        if (address == null || (address.length != 4 && address.length != 16)) {
            return false;
        }
        byte[] normalized = normalizeMapped(address);
        return networks.stream().anyMatch(network -> network.matches(normalized));
    }

    /** 校验输入长度并拆分条目，防止非空的无效配置被当成不限。 */
    private static List<String> splitRules(String value, String separator) {
        if (value == null) {
            return List.of();
        }
        if (value.length() > MAX_RULES_LENGTH) {
            throw new IllegalArgumentException("访问规则最多 2000 字符");
        }
        if (value.isBlank()) {
            return List.of();
        }
        List<String> rules = Arrays.stream(value.split(separator)).map(String::strip)
            .filter(rule -> !rule.isEmpty()).toList();
        if (rules.isEmpty()) {
            throw new IllegalArgumentException("访问规则不能为空条目");
        }
        return rules;
    }

    /** 解析纯字面量网络并将可等价表达的 IPv4-mapped 网段统一为 IPv4。 */
    private static Network parseNetwork(String rule) {
        String[] parts = rule.split("/", -1);
        if (parts.length > 2) {
            throw new IllegalArgumentException("允许 IP 格式不合法");
        }
        byte[] address = parseAddress(parts[0]);
        int prefix = address.length * Byte.SIZE;
        if (parts.length == 2) {
            if (!parts[1].matches("0|[1-9][0-9]{0,2}")) {
                throw new IllegalArgumentException("CIDR 前缀不合法");
            }
            prefix = Integer.parseInt(parts[1]);
            if (prefix > address.length * Byte.SIZE) {
                throw new IllegalArgumentException("CIDR 前缀越界");
            }
        }
        byte[] normalized = normalizeMapped(address);
        if (normalized.length != address.length) {
            if (prefix < 96) {
                throw new IllegalArgumentException("IPv4-mapped CIDR 前缀不能小于 96");
            }
            prefix -= 96;
        }
        return new Network(normalized, prefix);
    }

    /** 只按数值解析地址，IPv6 压缩和末尾 IPv4 均不经过 InetAddress/DNS。 */
    private static byte[] parseAddress(String literal) {
        if (literal.matches("(?:0|[1-9][0-9]{0,2})(?:\\.(?:0|[1-9][0-9]{0,2})){3}")
            && IPAddress.isValidIPv4(literal)) {
            String[] octets = literal.split("\\.");
            byte[] bytes = new byte[4];
            for (int i = 0; i < octets.length; i++) {
                if (!octets[i].matches("0|[1-9][0-9]{0,2}")) {
                    throw new IllegalArgumentException("IPv4 不允许缩写或前导零");
                }
                bytes[i] = (byte) Integer.parseInt(octets[i]);
            }
            return bytes;
        }
        if (!literal.matches("[0-9a-fA-F:.]+") || !IPAddress.isValidIPv6(literal)) {
            throw new IllegalArgumentException("允许 IP 必须是 IPv4、IPv6 或 CIDR");
        }
        String[] halves = literal.split("::", -1);
        List<Integer> left = ipv6Words(halves[0]);
        List<Integer> right = halves.length == 2 ? ipv6Words(halves[1]) : List.of();
        List<Integer> words = new ArrayList<>(left);
        if (halves.length == 2) {
            for (int i = left.size() + right.size(); i < 8; i++) {
                words.add(0);
            }
        }
        words.addAll(right);
        byte[] bytes = new byte[16];
        for (int i = 0; i < words.size(); i++) {
            bytes[2 * i] = (byte) (words.get(i) >>> Byte.SIZE);
            bytes[2 * i + 1] = words.get(i).byteValue();
        }
        return bytes;
    }

    /** 将已通过 IPv6 语法校验的半段转换成 16 位字，严格解析末尾 IPv4。 */
    private static List<Integer> ipv6Words(String half) {
        List<Integer> words = new ArrayList<>();
        if (!half.isEmpty()) {
            for (String word : half.split(":")) {
                if (word.contains(".")) {
                    byte[] ipv4 = parseAddress(word);
                    words.add((Byte.toUnsignedInt(ipv4[0]) << Byte.SIZE) | Byte.toUnsignedInt(ipv4[1]));
                    words.add((Byte.toUnsignedInt(ipv4[2]) << Byte.SIZE) | Byte.toUnsignedInt(ipv4[3]));
                } else {
                    words.add(Integer.parseInt(word, 16));
                }
            }
        }
        return words;
    }

    /** IPv4-mapped IPv6 与原生 IPv4 使用同一地址族，其他 IPv6 保持不变。 */
    private static byte[] normalizeMapped(byte[] address) {
        if (address.length == 16 && address[10] == (byte) 0xff && address[11] == (byte) 0xff) {
            for (int i = 0; i < 10; i++) {
                if (address[i] != 0) {
                    return address;
                }
            }
            return Arrays.copyOfRange(address, 12, 16);
        }
        return address;
    }

    /**
     * 已校验的地址及前缀，只进行同地址族的按位匹配。
     *
     * @since 6.1.0
     */
    private record Network(byte[] address, int prefix) {
        /** 比较前缀有效位，不要求配置的主机位已清零。 */
        private boolean matches(byte[] candidate) {
            if (address.length != candidate.length) {
                return false;
            }
            for (int bit = 0; bit < prefix; bit++) {
                int mask = 1 << (7 - bit % Byte.SIZE);
                if ((address[bit / Byte.SIZE] & mask) != (candidate[bit / Byte.SIZE] & mask)) {
                    return false;
                }
            }
            return true;
        }
    }
}
