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

package dev.macula.boot.starter.security.passkey;

import dev.macula.boot.starter.security.authorization.CheckOwnerAccessDeniedException;
import dev.macula.boot.starter.security.config.SecurityProperties;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;

/**
 * Stateless HMAC-SHA256 credentials with unambiguous field encoding and a bounded lifetime.
 * Instances are thread-safe; a fresh Mac is used per operation.
 *
 * @author Macula
 * @since 6.1.0
 */
public class HmacPasskeyService implements PasskeyService {

    private static final int MAX_IDENTITY_LENGTH = 1024;
    private final SecretKeySpec key;
    private final Clock clock;
    private final Duration ttl;

    /**
     * Creates the default issuer; disabled configuration cannot issue or verify credentials.
     * @param properties immutable signing settings captured at startup
     * @param clock expiration time source
     */
    public HmacPasskeyService(SecurityProperties.Passkey properties, Clock clock) {
        this.clock = clock;
        this.ttl = properties.getTtl();
        if (!properties.isEnabled()) {
            this.key = null;
            return;
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero() || ttl.getSeconds() < 1) {
            throw new IllegalArgumentException("Passkey ttl must be at least one second");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(properties.getSecretBase64() == null ? "" : properties.getSecretBase64());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid passkey signing configuration");
        }
        if (bytes.length < 32) {
            throw new IllegalArgumentException("Passkey signing key must contain at least 32 bytes");
        }
        this.key = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Override
    public String issue(String subject, String resourceId) {
        if (key == null || !validIdentity(subject) || !validIdentity(resourceId)) {
            throw new CheckOwnerAccessDeniedException("Resource credential cannot be issued");
        }
        long expiresAt;
        try {
            expiresAt = Math.addExact(clock.instant().getEpochSecond(), ttl.getSeconds());
        } catch (ArithmeticException ex) {
            throw new CheckOwnerAccessDeniedException("Resource credential cannot be issued");
        }
        return "v1." + expiresAt + "." + Base64.getUrlEncoder().withoutPadding()
            .encodeToString(sign(subject, resourceId, expiresAt));
    }

    @Override
    public boolean verify(String subject, String resourceId, String passkey) {
        if (key == null || !validIdentity(subject) || !validIdentity(resourceId)
            || passkey == null || passkey.length() > 128) {
            return false;
        }
        String[] parts = passkey.split("\\.", -1);
        if (parts.length != 3 || !"v1".equals(parts[0]) || !parts[1].matches("[0-9]{1,19}")) {
            return false;
        }
        try {
            long expiresAt = Long.parseLong(parts[1]);
            if (expiresAt <= clock.instant().getEpochSecond()) {
                return false;
            }
            byte[] supplied = Base64.getUrlDecoder().decode(parts[2]);
            return supplied.length == 32 && MessageDigest.isEqual(sign(subject, resourceId, expiresAt), supplied);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private boolean validIdentity(String value) {
        return value != null && !value.isBlank() && value.length() <= MAX_IDENTITY_LENGTH;
    }

    private byte[] sign(String subject, String resourceId, long expiresAt) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                output.writeInt(1);
                writeField(output, subject);
                writeField(output, resourceId);
                output.writeLong(expiresAt);
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            return mac.doFinal(bytes.toByteArray());
        } catch (GeneralSecurityException | IOException ex) {
            throw new IllegalStateException("Resource signing operation failed");
        }
    }

    private void writeField(DataOutputStream output, String value) throws IOException {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        output.writeInt(encoded.length);
        output.write(encoded);
    }
}
