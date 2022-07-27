package dev.macula.example.consumer.feign.configuration;

import dev.macula.boot.starter.cloud.feign.interceptor.KongApiInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class GapiConfiguration {
    @Value("${gapi.username}")
    private String username;
    @Value("${gapi.secret}")
    private String secret;

    @Bean
    KongApiInterceptor kongApiInterceptor() {
        return new KongApiInterceptor(username, secret);
    }

}
