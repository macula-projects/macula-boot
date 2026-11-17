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


package dev.macula.boot.starter.cloud.gateway.config;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.nio.file.Path;
import java.security.KeyStore;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 验证 JWT 签名材料配置、旧默认兼容以及自定义 Bean 退让。
 *
 * @author Rain
 * @since 6.1.0
 */
@ExtendWith(OutputCaptureExtension.class)
class JwtConfigurationTest {
    @TempDir
    static Path directory;
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(JwtConfiguration.class);

    /** 仅在测试临时目录生成私钥，不提交签名材料。 */
    @BeforeAll
    static void generateKeyStores() throws Exception {
        for (String type : new String[] {"JKS", "PKCS12"}) {
            var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "test", "-keyalg", "RSA", "-keysize", "2048", "-dname", "CN=LocalTest",
                "-validity", "1", "-storetype", type, "-keystore", directory.resolve(type).toString(),
                "-storepass", "test-store-password", "-keypass", "test-store-password", "-noprompt")
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            assertThat(process.waitFor()).isZero();
        }
        var ec = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
            "-genkeypair", "-alias", "test", "-keyalg", "EC", "-dname", "CN=LocalTest",
            "-validity", "1", "-storetype", "PKCS12", "-keystore", directory.resolve("EC").toString(),
            "-storepass", "test-store-password", "-keypass", "test-store-password", "-noprompt")
            .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        assertThat(ec.waitFor()).isZero();
        KeyStore publicOnly = KeyStore.getInstance("JKS");
        publicOnly.load(null, null);
        KeyStore source = KeyStore.getInstance("JKS");
        try (var input = java.nio.file.Files.newInputStream(directory.resolve("JKS"))) {
            source.load(input, "test-store-password".toCharArray());
        }
        publicOnly.setCertificateEntry("test", source.getCertificate("test"));
        try (var output = java.nio.file.Files.newOutputStream(directory.resolve("public-only"))) {
            publicOnly.store(output, "test-store-password".toCharArray());
        }
    }

    /** 无新增配置时仍使用旧签发密钥，并提示生产替换而不泄漏密码。 */
    @Test
    void preservesLegacySigningKey(CapturedOutput output) {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(JwtEncoder.class).hasSingleBean(JWKSource.class);
            try {
                KeyStore store = KeyStore.getInstance("JKS");
                var password = new GatewayProperties.Jwt().getKeyStorePassword().toCharArray();
                try (var input = getClass().getResourceAsStream("/jwk/jose.jks")) {
                    store.load(input, password);
                }
                assertSignature(context.getBean(JwtEncoder.class), (RSAPublicKey)store.getCertificate("jose").getPublicKey());
            } catch (Exception failure) {
                throw new AssertionError(failure);
            }
        });
        assertThat(output.getAll()).contains("公开内置 JWT").doesNotContain(new GatewayProperties.Jwt().getKeyStorePassword());
    }

    /** 显式外部 KeyStore 同时支持 JKS 和 PKCS12，且不是仅成功加载配置。 */
    @Test
    void signsWithConfiguredKeyStores() {
        for (String type : new String[] {"JKS", "PKCS12"}) {
            configured(type).run(context -> {
                assertThat(context).hasNotFailed();
                try {
                    var source = context.getBean(JWKSource.class);
                    RSAKey rsa = (RSAKey)source.get(new JWKSelector(new JWKMatcher.Builder().build()), null).get(0);
                    assertSignature(context.getBean(JwtEncoder.class), rsa.toRSAPublicKey());
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            });
        }
    }

    /** 返回显式配置的上下文运行器。 */
    private ApplicationContextRunner configured(String type) {
        return runner.withPropertyValues("macula.gateway.jwt.key-store=" + directory.resolve(type).toUri(),
            "macula.gateway.jwt.key-store-type=" + type, "macula.gateway.jwt.key-store-password=test-store-password",
            "macula.gateway.jwt.key-alias=test");
    }

    /** 通过真正的 RS256 签发和验签检查指定公钥。 */
    private void assertSignature(JwtEncoder encoder, RSAPublicKey key) throws Exception {
        var jwt = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(),
            JwtClaimsSet.builder().subject("alice").build()));
        assertThat(JWSObject.parse(jwt.getTokenValue()).verify(new RSASSAVerifier(key))).isTrue();
    }

    /** 显式配置错误不得退回旧密钥，错误也不能泄漏密码。 */
    @Test
    void failsWithoutFallbackOrSecretDisclosure(CapturedOutput output) {
        for (String property : new String[] {"key-alias=absent", "key-store-password=private-test-value",
            "key-password=private-key-value", "key-store=file:/does-not-exist", "key-store=https://example.invalid/key"}) {
            configured("JKS").withPropertyValues("macula.gateway.jwt." + property)
                .run(context -> assertThat(context).hasFailed());
        }
        assertThat(output.getAll()).doesNotContain("private-test-value", "private-key-value");
    }

    /** 用户 source 优先；仅自定义编码器时仍保留框架 source，与原装配一致。 */
    @Test
    void backsOffForCustomSigningBeans() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        runner.withBean("customEncoder", JwtEncoder.class, () -> encoder).run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(JwtEncoder.class).hasSingleBean(JWKSource.class);
                assertThat(context.getBean(JwtEncoder.class)).isSameAs(encoder);
            });
        JWKSource<?> source = (selector, securityContext) -> java.util.List.of();
        runner.withPropertyValues("macula.gateway.jwt.key-store=invalid")
            .withBean("customSource", JWKSource.class, () -> source)
            .run(context -> assertThat(context).hasNotFailed().hasSingleBean(JWKSource.class).hasSingleBean(JwtEncoder.class));
    }

    /** 自定义编码器依赖框架 source 时必须能启动并实际签名验签。 */
    @Test
    void customEncoderCanReuseFrameworkSource() {
        new ApplicationContextRunner().withUserConfiguration(CustomEncoderConfiguration.class, JwtConfiguration.class)
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(JWKSource.class).hasSingleBean(JwtEncoder.class);
                var source = context.getBean(JWKSource.class);
                RSAKey rsa = (RSAKey)source.get(new JWKSelector(new JWKMatcher.Builder().build()), null).get(0);
                assertSignature(context.getBean(JwtEncoder.class), rsa.toRSAPublicKey());
            });
    }

    /**
     * 用户只替换编码器，保留容器中的签名来源。
     * @since 6.1.0
     */
    @org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
    static class CustomEncoderConfiguration {
        /** 明确声明对框架 source 的依赖，以覆盖条件装配回归。 */
        @org.springframework.context.annotation.Bean
        JwtEncoder customEncoder(JWKSource<com.nimbusds.jose.proc.SecurityContext> source) {
            return new org.springframework.security.oauth2.jwt.NimbusJwtEncoder(source);
        }
    }
    /** 只有公钥证书的条目不满足签发要求。 */
    @Test
    void rejectsPublicOnlyEntry() {
        configured("JKS").withPropertyValues("macula.gateway.jwt.key-store=" + directory.resolve("public-only").toUri())
            .run(context -> assertThat(context).hasFailed());
    }
    /** RS256 签发配置不能加载 EC 私钥。 */
    @Test
    void rejectsNonRsaPrivateKey() {
        configured("PKCS12").withPropertyValues("macula.gateway.jwt.key-store=" + directory.resolve("EC").toUri())
            .run(context -> assertThat(context).hasFailed());
    }
}
