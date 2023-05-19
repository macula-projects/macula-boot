package dev.macula.boot.starter.cloud.gateway.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyStore;

/**
 * The Jwt configuration.
 *
 * @author felord.cn
 * @since 1.0.0
 */
@Configuration
public class JwtConfiguration {

    /**
     * 加载JWK资源
     *
     * @return the jwk source
     */
    @SneakyThrows
    @Bean
    @ConditionalOnMissingBean(JWKSource.class)
    public JWKSource<SecurityContext> jwkSource() {
        //TODO 这里优化到配置
        String path = "jwk/jose.jks";
        String alias = "jose";
        String pass = "felord.cn";

        ClassPathResource resource = new ClassPathResource(path);
        KeyStore jks = KeyStore.getInstance("jks");
        char[] pin = pass.toCharArray();
        jks.load(resource.getInputStream(), pin);
        RSAKey rsaKey = RSAKey.load(jks, alias, pin);
        return (jwkSelector, securityContext) -> jwkSelector.select(new JWKSet(rsaKey));
    }

    /**
     * JWT编码器，这个是网关自己生成的JWT给下游的微服务，与IAM无关
     *
     * @param jwkSource
     * @return
     */
    @Bean
    @ConditionalOnMissingBean(JwtEncoder.class)
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }
}
