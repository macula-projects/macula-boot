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
package dev.macula.boot.starter.tinyid.remote;

import com.sun.net.httpserver.HttpServer;
import dev.macula.boot.starter.tinyid.config.TinyIdAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.assertj.core.api.Assertions.*;

/**
 * 使用本地 HTTP 服务验证 Feign 请求路径及与 StripPrefix 后路径匹配的签名。
 * @author Rain
 * @since 6.1.0
 */
class TinyIdFeignClientIT {

    @Test
    void signsServicePathAndHandlesRemoteFailure() throws Exception {
        AtomicReference<String> received = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> date = new AtomicReference<>();
        AtomicReference<String> accept = new AtomicReference<>();
        AtomicReference<byte[]> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/tinyid/api/v1/id/nextSegmentIdSimple", exchange -> {
            received.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().toASCIIString());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            date.set(exchange.getRequestHeaders().getFirst("Date"));
            accept.set(exchange.getRequestHeaders().getFirst("Accept"));
            body.set(exchange.getRequestBody().readAllBytes());
            boolean failure = exchange.getRequestURI().getQuery().contains("failure");
            byte[] response = (failure
                ? "{\"success\":false,\"code\":\"ID503\",\"msg\":\"business not found\",\"cause\":\"safe details\"}"
                : "0,20,100,1,0").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", failure ? "application/json" : "text/plain");
            int status = failure ? 500 : 200;
            exchange.sendResponseHeaders(status, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            new ApplicationContextRunner()
                .withSystemProperties("macula.cloud.app-key=test-app", "macula.cloud.secret-key=test-only-secret")
                .withConfiguration(AutoConfigurations.of(TinyIdAutoConfiguration.class, FeignAutoConfiguration.class))
                .withBean(feign.codec.ErrorDecoder.class,
                    dev.macula.boot.starter.feign.codec.OpenFeignErrorDecoder::new)
                .withPropertyValues("macula.cloud.endpoint=http://127.0.0.1:" + server.getAddress().getPort(),
                    "macula.cloud.app-key=test-app", "macula.cloud.secret-key=test-only-secret")
                .run(context -> {
                    TinyIdFeignClient client = context.getBean(TinyIdFeignClient.class);
                    assertThat(client.nextSegmentId("sales order&archive")).isEqualTo("0,20,100,1,0");
                    assertThat(accept.get()).contains("text/plain", "application/json");
                    assertThat(received.get()).startsWith("POST /tinyid/api/v1/id/nextSegmentIdSimple?")
                        .contains("bizType=sales%20order%26archive").doesNotContain("token=");
                    String digest = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(body.get()));
                    String serviceRequest = received.get().replace("POST /tinyid/", "POST /");
                    String input = "date: " + date.get() + "\n" + serviceRequest + " HTTP/1.1\ndigest: SHA-256=" + digest;
                    Mac mac = Mac.getInstance("HmacSHA256");
                    mac.init(new SecretKeySpec("test-only-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                    assertThat(authorization.get()).contains("username=\"test-app\"",
                        "signature=\"" + Base64.getEncoder().encodeToString(mac.doFinal(input.getBytes(StandardCharsets.UTF_8))) + "\"");
                    assertThatThrownBy(() -> client.nextSegmentId("failure"))
                        .isInstanceOfSatisfying(dev.macula.boot.exception.BizException.class, error -> {
                            assertThat(error.getCode()).isEqualTo("ID503");
                            assertThat(error.getMsg()).isEqualTo("business not found");
                            assertThat(error).hasMessage("safe details");
                        });
                });
        } finally {
            server.stop(0);
        }
    }
}
