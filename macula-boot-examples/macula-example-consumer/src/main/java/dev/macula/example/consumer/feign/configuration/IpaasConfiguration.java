package dev.macula.example.consumer.feign.configuration;

import dev.macula.boot.starter.cloud.feign.interceptor.KongApiInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class IpaasConfiguration {

    @Value("${ipaas.username}")
    private String username;
    @Value("${ipaas.secret}")
    private String secret;
    @Value("${ipaas.appKey}")
    private String appKey;

    @Bean
    KongApiInterceptor ipaasInterceptor() {
        return new KongApiInterceptor(username, secret, appKey);
    }
}
