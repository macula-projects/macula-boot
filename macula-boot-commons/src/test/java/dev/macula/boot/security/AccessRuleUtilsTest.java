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

import java.net.InetAddress;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 验证多行 URL、严格 IP/CIDR 与 mapped IPv6 的解析及失败边界。
 *
 * @author Rain
 * @since 6.1.0
 */
class AccessRuleUtilsTest {
    /** 公共 URL 匹配保留 Ant 语义和旧分隔方式，空白不限、缺失目标或不匹配拒绝。 */
    @Test
    void matchesUrlRulesWithCallerProvidedMatcher() {
        var matcher = new org.springframework.util.AntPathMatcher();
        String rules = " POST:/orders,\r\n GET:/orders/** ";
        assertThat(AccessRuleUtils.matchesUrl(rules, "GET:/orders/42", matcher::match)).isTrue();
        assertThat(AccessRuleUtils.matchesUrl(rules, "POST:/orders", matcher::match)).isTrue();
        assertThat(AccessRuleUtils.matchesUrl(rules, "DELETE:/orders/42", matcher::match)).isFalse();
        assertThat(AccessRuleUtils.matchesUrl(rules, null, matcher::match)).isFalse();
        assertThat(AccessRuleUtils.matchesUrl(null, null, matcher::match)).isTrue();
        assertThat(AccessRuleUtils.matchesUrl(" \r\n ", "GET:/orders", matcher::match)).isTrue();
        assertThatIllegalArgumentException().isThrownBy(() -> AccessRuleUtils.matchesUrl(",,", "GET:/orders", matcher::match));
        assertThatIllegalArgumentException().isThrownBy(() -> AccessRuleUtils.matchesUrl("x".repeat(2001), "GET:/orders", matcher::match));
    }

    /** 历史逗号和三种换行共同规范化，空白仍表示不限。 */
    @Test
    void normalizesLegacyAndMultilineUrls() {
        assertThat(AccessRuleUtils.normalizeUrls(" GET:/a/**,\r\n POST:/b \rPUT:/c\n\n"))
            .isEqualTo("GET:/a/**\nPOST:/b\nPUT:/c");
        assertThat(AccessRuleUtils.normalizeUrls(null)).isEmpty();
        assertThat(AccessRuleUtils.normalizeUrls(" \r\n ")).isEmpty();
        assertThatIllegalArgumentException().isThrownBy(() -> AccessRuleUtils.normalizeUrls(" ,,,\n"));
        assertThatIllegalArgumentException().isThrownBy(() -> AccessRuleUtils.normalizeUrls("x".repeat(2001)));
    }

    /** 不同地址族和 CIDR 边界按前缀匹配，不把 IPv6 的 /0 用于 IPv4。 */
    @Test
    void matchesLiteralAddressesAndNetworkBoundaries() throws Exception {
        assertThat(matches("192.0.2.0/24", "192.0.2.255")).isTrue();
        assertThat(matches("192.0.2.0/24", "192.0.3.0")).isFalse();
        assertThat(matches("192.0.2.7/32", "192.0.2.7")).isTrue();
        assertThat(matches("192.0.2.7/32", "192.0.2.8")).isFalse();
        assertThat(matches("0.0.0.0/0", "203.0.113.9")).isTrue();
        assertThat(matches("::/0", "192.0.2.7")).isFalse();
        assertThat(matches("2001:db8::/32", "2001:db8:abcd::1")).isTrue();
        assertThat(matches("2001:db8::/32", "2001:db9::1")).isFalse();
        assertThat(matches("::1/128", "::1")).isTrue();
        assertThat(matches("::1/128", "::2")).isFalse();
        assertThat(matches("2001:db8::2\r\n192.0.2.7", "192.0.2.7")).isTrue();
        assertThat(AccessRuleUtils.normalizeIps(" 192.0.2.7 \r\n\r2001:db8::1 "))
            .isEqualTo("192.0.2.7\n2001:db8::1");
    }

    /** mapped 地址及可等价表达的前缀归一为 IPv4，模糊网段拒绝。 */
    @Test
    void normalizesMappedIpv6WithoutBroadeningNetworks() throws Exception {
        assertThat(matches("::ffff:192.0.2.7", "192.0.2.7")).isTrue();
        assertThat(matches("::ffff:c000:0200/120", "192.0.2.9")).isTrue();
        assertThat(matches("::ffff:c000:0200/120", "192.0.3.9")).isFalse();
        byte[] mapped = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1, -1, -64, 0, 2, 7};
        assertThat(AccessRuleUtils.matchesIp("192.0.2.0/24", mapped)).isTrue();
        assertThatIllegalArgumentException().isThrownBy(() -> AccessRuleUtils.normalizeIps("::ffff:192.0.2.1/95"));
    }

    /** 非空错误配置整体失败，不能用已命中的前一条掩盖错误条目。 */
    @Test
    void rejectsMalformedRulesWithoutDnsOrPermissiveFallback() throws Exception {
        for (String invalid : new String[] {"example.invalid", "192.0.2.1:80", "[::1]", "fe80::1%eth0",
            "192.0.2.*", "999.0.0.1", "127.1", "01.2.3.4", "::ffff:01.2.3.4", "1::2::3",
            "1:2:3:4:5:6:7:8:9", "1.2.3.4/33", "::/129", "::/-1", "::/", "::/1/2", "::/+1",
            "::/01", "192.0.2.1,192.0.2.2", ",,,"}) {
            assertThatIllegalArgumentException().as(invalid).isThrownBy(() -> AccessRuleUtils.normalizeIps(invalid));
        }
        byte[] address = InetAddress.getByName("192.0.2.7").getAddress();
        assertThatIllegalArgumentException().isThrownBy(() -> AccessRuleUtils.matchesIp("192.0.2.7\ninvalid", address));
        assertThat(AccessRuleUtils.matchesIp(null, null)).isTrue();
        assertThat(AccessRuleUtils.matchesIp(" \n", null)).isTrue();
        assertThat(AccessRuleUtils.matchesIp("192.0.2.7", null)).isFalse();
        assertThat(AccessRuleUtils.matchesIp("192.0.2.7", new byte[3])).isFalse();
        assertThatIllegalArgumentException().isThrownBy(() -> AccessRuleUtils.normalizeIps(" ".repeat(2001)));
    }

    /** 仅为测试中固定的 IP 字面量提供地址字节。 */
    private boolean matches(String rules, String literal) throws Exception {
        return AccessRuleUtils.matchesIp(rules, InetAddress.getByName(literal).getAddress());
    }
}
