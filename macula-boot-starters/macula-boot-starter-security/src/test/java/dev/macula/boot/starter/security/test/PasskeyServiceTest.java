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

package dev.macula.boot.starter.security.test;

import dev.macula.boot.starter.security.config.SecurityProperties;
import dev.macula.boot.starter.security.passkey.HmacPasskeyService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks deterministic expiration, mainline token compatibility, identity binding and concurrent signing.
 *
 * @author Rain
 * @since 5.0.21
 */
class PasskeyServiceTest {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);
    private static final String MAINLINE_GOLDEN_TOKEN =
        "v1.1793491500.XQs-g6C8KpQN7Rtdlli16Lfy5T1s_IrIxwkUvUQLHQA";

    static SecurityProperties.Passkey settings() {
        SecurityProperties.Passkey properties = new SecurityProperties.Passkey();
        properties.setEnabled(true);
        properties.setSecretBase64(Base64.getEncoder().encodeToString(new byte[32]));
        return properties;
    }

    @Test
    void matchesMainlineGoldenTokenAndBindsUserResourceAndExpiry() {
        HmacPasskeyService service = new HmacPasskeyService(settings(), CLOCK);
        String token = service.issue("alice", "123");
        assertThat(token).isEqualTo(MAINLINE_GOLDEN_TOKEN);
        assertThat(service.verify("alice", "123", MAINLINE_GOLDEN_TOKEN)).isTrue();
        assertThat(service.verify("bob", "123", token)).isFalse();
        assertThat(service.verify("alice", "124", token)).isFalse();
        assertThat(service.verify("alice", "123", token + "x")).isFalse();
        assertThat(new HmacPasskeyService(settings(), Clock.offset(CLOCK, Duration.ofSeconds(299)))
            .verify("alice", "123", token)).isTrue();
        assertThat(new HmacPasskeyService(settings(), Clock.offset(CLOCK, Duration.ofMinutes(5)))
            .verify("alice", "123", token)).isFalse();
    }

    @Test
    void validatesConfigurationAndRejectsDisabledService() {
        HmacPasskeyService disabled = new HmacPasskeyService(new SecurityProperties.Passkey(), CLOCK);
        assertThat(disabled.verify("alice", "123", "anything")).isFalse();
        assertThatThrownBy(() -> disabled.issue("alice", "123")).isInstanceOf(RuntimeException.class);
        SecurityProperties.Passkey invalid = settings();
        invalid.setSecretBase64("not-a-key");
        assertThatThrownBy(() -> new HmacPasskeyService(invalid, CLOCK))
            .isInstanceOf(IllegalArgumentException.class).hasMessageNotContaining("not-a-key");
        invalid.setSecretBase64(Base64.getEncoder().encodeToString(new byte[31]));
        assertThatThrownBy(() -> new HmacPasskeyService(invalid, CLOCK)).isInstanceOf(IllegalArgumentException.class);
        invalid.setTtl(Duration.ZERO);
        assertThatThrownBy(() -> new HmacPasskeyService(invalid, CLOCK)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsMalformedAmbiguousOrExcessiveInput() {
        HmacPasskeyService service = new HmacPasskeyService(settings(), CLOCK);
        String[] invalid = {"", "v2.12.ab", "v1.-1.ab", "v1.9999999999999999999.ab", repeat("x", 129)};
        for (String token : invalid) {
            assertThat(service.verify("alice", "123", token)).isFalse();
        }
        assertThat(service.verify("alice", "123", null)).isFalse();
        assertThatThrownBy(() -> service.issue("alice", " ")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> service.issue(repeat("a", 1025), "123")).isInstanceOf(RuntimeException.class);
        assertThat(service.verify("a", "bc", service.issue("ab", "c"))).isFalse();
    }

    @Test
    void supportsConcurrentCallsWithoutSharingMacState() {
        HmacPasskeyService service = new HmacPasskeyService(settings(), CLOCK);
        assertThat(IntStream.range(0, 100).parallel()
            .allMatch(i -> service.verify("alice", "id" + i, service.issue("alice", "id" + i)))).isTrue();
    }

    private static String repeat(String value, int count) {
        StringBuilder result = new StringBuilder(value.length() * count);
        for (int i = 0; i < count; i++) {
            result.append(value);
        }
        return result.toString();
    }
}
