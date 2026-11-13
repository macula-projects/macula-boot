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


package dev.macula.example.gateway.crypto;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.macula.boot.result.Result;
import dev.macula.boot.starter.cloud.gateway.config.GatewayProperties;
import dev.macula.boot.starter.cloud.gateway.filter.BodyGlobalFilter;
import dev.macula.boot.starter.cloud.gateway.filter.CryptoGlobalFilter;
import dev.macula.boot.starter.cloud.gateway.filter.SignCheckGlobalFilter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import dev.macula.boot.starter.cloud.gateway.filter.ProtectUrlsEndpointFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.StripPrefixGatewayFilterFactory;
import org.springframework.cloud.gateway.handler.FilteringWebHandler;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.server.HttpServer;

import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_PREDICATE_PATH_CONTAINER_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

/**
 * 使用实际浏览器适配代码、Gateway 过滤链和 HTTP 下游桩验证加密签名及普通请求。
 * 不加载业务应用自动配置，不连接 Nacos、Redis、OAuth 或真实 consumer/provider。
 *
 * @author Codex
 * @since 6.1.0
 */
class CryptoDemoIT {
    private static final String API = "/consumer/api/v1/consumer";
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final CryptoLocaleServiceImpl CRYPTO = new CryptoLocaleServiceImpl();
    private static final AtomicReference<String> DOWNSTREAM_URI = new AtomicReference<>();
    private static final AtomicReference<String> DOWNSTREAM_BODY = new AtomicReference<>();
    private static final AtomicReference<String> DOWNSTREAM_KEY = new AtomicReference<>();
    private static DisposableServer downstream;
    private static DisposableServer gateway;
    private static final java.net.http.HttpClient CLIENT = java.net.http.HttpClient.newHttpClient();

    /** 启动真实过滤器和隔离的 HTTP 回声桩；不触发外部自动配置。 */
    @BeforeAll
    static void startServers() throws Exception {
        CRYPTO.afterPropertiesSet();
        downstream = HttpServer.create().host("127.0.0.1").port(0).handle((request, response) -> {
            DOWNSTREAM_URI.set(request.uri());
            DOWNSTREAM_KEY.set(request.requestHeaders().get("sm4-key"));
            return request.receive().aggregate().asString(StandardCharsets.UTF_8).defaultIfEmpty("")
                .flatMap(body -> {
                    DOWNSTREAM_BODY.set(body);
                    Object data;
                    if (request.method().name().equals("GET")) {
                        String query = URI.create(request.uri()).getRawQuery();
                        String value = URLDecoder.decode(query.substring("str=".length()), StandardCharsets.UTF_8);
                        data = request.uri().startsWith("/api/v1/consumer/plain?") ? "Plain " + value : "Hello " + value + ",demo";
                    } else {
                        data = JSONUtil.parseObj(body);
                    }
                    return response.header("Content-Type", "application/json")
                        .sendString(Mono.just(JSONUtil.toJsonStr(Result.success(data)))).then();
                });
        }).bindNow(TIMEOUT);
        // 读取实际 local 配置，避免测试重写一份配置而漏掉示例 YAML 错误。
        StandardEnvironment environment = new StandardEnvironment();
        new YamlPropertySourceLoader().load("demo", new ClassPathResource("application.yml"))
            .stream().filter(source -> "local".equals(source.getProperty("spring.config.activate.on-profile")))
            .forEach(source -> environment.getPropertySources().addFirst(source));
        GatewayProperties properties = Binder.get(environment)
            .bind("macula.gateway", Bindable.of(GatewayProperties.class)).get();
        assertThat(properties.isForceCrypto()).isTrue();
        assertThat(properties.isCryptoSwitch()).isTrue();
        assertThat(properties.isSignSwitch()).isTrue();
        assertThat(properties.isForceSign()).isTrue();
        assertThat(properties.getProtectUrls().getSign()).containsExactly(API + "/echo/*", API + "/user");
        assertThat(Binder.get(environment).bind("macula.gateway.security.ignore-urls", Bindable.listOf(String.class)).get())
            .contains("/crypto-demo/**", API + "/plain").doesNotContain(API + "/user");
        GlobalFilter proxy = (exchange, chain) -> HttpClient.create()
            .request(io.netty.handler.codec.http.HttpMethod.valueOf(exchange.getRequest().getMethod().name()))
            .uri("http://127.0.0.1:" + downstream.port() + exchange.getRequest().getURI().getRawPath()
                + (exchange.getRequest().getURI().getRawQuery() == null ? "" : "?" + exchange.getRequest().getURI().getRawQuery()))
            .send((request, outbound) -> outbound.sendByteArray(exchange.getRequest().getBody().map(buffer -> {
                byte[] bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                DataBufferUtils.release(buffer);
                return bytes;
            })))
            .responseSingle((response, bytes) -> bytes.asByteArray()).flatMap(bytes -> {
                exchange.getResponse().getHeaders().set("Content-Type", "application/json");
                return exchange.getResponse().writeWith(Flux.just(exchange.getResponse().bufferFactory().wrap(bytes)));
            });
        FilteringWebHandler filters = new FilteringWebHandler(
            List.of(new BodyGlobalFilter(), new SignCheckGlobalFilter(CRYPTO, properties, nonceStore()),
                new CryptoGlobalFilter(CRYPTO, properties), proxy), false);
        Route route = Route.async().id("consumer-test").uri("http://127.0.0.1:" + downstream.port())
            .predicate(exchange -> true)
            .filter(new OrderedGatewayFilter(new StripPrefixGatewayFilterFactory()
                .apply(new StripPrefixGatewayFilterFactory.Config()), 1)).build();
        var handler = WebHttpHandlerBuilder.webHandler(exchange -> {
            if (!exchange.getRequest().getPath().value().startsWith("/consumer/")) {
                exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
                return exchange.getResponse().setComplete();
            }
            exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, route);
            exchange.getAttributes().put(GATEWAY_PREDICATE_PATH_CONTAINER_ATTR,
                PathContainer.parsePath(exchange.getRequest().getURI().getRawPath()));
            return filters.handle(exchange);
        }).filter(new ProtectUrlsEndpointFilter(CRYPTO, properties, null)).build();
        gateway = HttpServer.create().host("127.0.0.1").port(0)
            .handle(new ReactorHttpHandlerAdapter(handler)).bindNow(TIMEOUT);
    }

    /** 以测试替身模拟顺序 nonce 查重，不证明真实 Redis 连通性、TTL 或并发原子性。 */
    @SuppressWarnings("unchecked")
    private static RedisTemplate<String, Object> nonceStore() {
        Set<String> seen = ConcurrentHashMap.newKeySet();
        RedisTemplate<String, Object> redis = mock(RedisTemplate.class);
        ValueOperations<String, Object> values = mock(ValueOperations.class);
        when(redis.hasKey(anyString())).thenAnswer(call -> seen.contains(call.getArgument(0)));
        when(redis.opsForValue()).thenReturn(values);
        doAnswer(call -> {
            seen.add(call.getArgument(0));
            return null;
        })
            .when(values).set(anyString(), eq("x"), any(Duration.class));
        return redis;
    }

    /** 释放两个临时 HTTP 监听器。 */
    @AfterAll
    static void stopServers() {
        if (gateway != null) {
            gateway.disposeNow(TIMEOUT);
        }
        if (downstream != null) {
            downstream.disposeNow(TIMEOUT);
        }
    }

    /** 使用现有内置端点读取公钥和完整原始保护路径。 */
    @Test
    void exposesExistingKeyAndProtectedUrls() throws Exception {
        assertThat(get("/gateway/protect/key").getStr("data")).isEqualTo(CRYPTO.getSm2PublicKey());
        JSONObject urls = get("/gateway/protect/urls").getJSONObject("data");
        assertThat(urls.getJSONArray("crypto").toList(String.class))
            .containsExactly(API + "/echo/*", API + "/user");
    }

    /** 业务侧 GET 只传普通 Query，实际 Axios 拦截器跨 HTTP 自动加解密。 */
    @Test
    void preservesPathVariableAndDecryptsQueryAndResponse() throws Exception {
        JSONObject browser = node(Map.of("op", "request", "kind", "get", "baseUrl", uri("/").toString(),
            "input", Map.of("name", "demo", "str", "你好 Macula 100%")));
        assertThat(DOWNSTREAM_URI.get()).startsWith("/api/v1/consumer/echo/demo?str=");
        assertThat(browser.getJSONObject("result").getStr("data")).isEqualTo("Hello 你好 Macula 100%,demo");
        assertThat(browser.getJSONArray("trace").toString()).contains(API + "/echo/demo?data=");
    }

    /** 业务 POST 只传用户 JSON，自动解密的响应 data 已是对象。 */
    @Test
    void decryptsBodyAndEncryptsObjectResponse() throws Exception {
        JSONObject browser = node(Map.of("op", "request", "kind", "post", "baseUrl", uri("/").toString(),
            "token", "test-only-token", "input", Map.of("userName", "demo-user", "age", 25)));
        assertThat(DOWNSTREAM_URI.get()).isEqualTo("/api/v1/consumer/user");
        assertThat(JSONUtil.parseObj(DOWNSTREAM_BODY.get()).getStr("userName")).isEqualTo("demo-user");
        JSONObject decoded = browser.getJSONObject("result").getJSONObject("data");
        assertThat(decoded.getInt("age")).isEqualTo(25);
        assertThat(decoded.getStr("userName")).isEqualTo("demo-user");
        assertThat(browser.getJSONArray("trace").toString()).doesNotContain("test-only-token");
    }

    /** 缺少密钥头时，即使路径含变量也必须在下游前拒绝。 */
    @Test
    void rejectsMissingCryptoHeaders() throws Exception {
        DOWNSTREAM_URI.set(null);
        assertThat(get(API + "/echo/demo?str=hello").getBool("success")).isFalse();
        assertThat(DOWNSTREAM_URI.get()).isNull();
    }

    /** 普通路径通过同一实例明文访问，下游收到原值且没有密钥头。 */
    @Test
    void preservesOrdinaryRequestWithoutProtection() throws Exception {
        JSONObject browser = node(Map.of("op", "request", "kind", "plain", "baseUrl", uri("/").toString(),
            "input", Map.of("str", "A&B+中文")));
        assertThat(browser.getJSONObject("result").getStr("data")).isEqualTo("Plain A&B+中文");
        assertThat(DOWNSTREAM_KEY.get()).isNull();
        assertThat(browser.getJSONArray("trace")).isEmpty();
    }

    /** 缺少签名、篡改密文、过期时间与重复 nonce 均须在下游之前拒绝。 */
    @Test
    void rejectsInvalidAndReplayedSignatures() throws Exception {
        JSONObject missing = wire();
        missing.getJSONObject("headers").remove("signature");
        assertRejected(missing, "缺少签名");
        JSONObject tampered = wire();
        tampered.set("url", tampered.getStr("url") + "&extra=tampered");
        assertRejected(tampered, "签名验证不通过");
        JSONObject expired = wire();
        expired.getJSONObject("headers").set("timestamp", "1");
        assertRejected(expired, "时间戳超时");
        JSONObject body = node(Map.of("op", "wire", "kind", "post", "baseUrl", uri("/").toString(),
            "input", Map.of("userName", "demo-user")));
        body.set("body", body.getStr("body") + " ");
        assertRejected(body, "签名验证不通过");
        JSONObject replayed = wire();
        assertThat(sendWire(replayed).getBool("success")).isTrue();
        assertRejected(replayed, "禁止重复请求");
    }

    /** 捕获实际 Axios 拦截器生成的完整传输请求，不泄露明文密钥。 */
    private JSONObject wire() throws Exception {
        return node(Map.of("op", "wire", "baseUrl", uri("/").toString(), "input", Map.of("str", "hello")));
    }

    /** 发送已捕获请求，用于验证服务端负向分支与原样重放。 */
    private JSONObject sendWire(JSONObject wire) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(wire.getStr("url"))).timeout(TIMEOUT);
        if ("POST".equals(wire.getStr("method"))) {
            builder.POST(HttpRequest.BodyPublishers.ofString(wire.getStr("body")));
        } else {
            builder.GET();
        }
        wire.getJSONObject("headers").forEach((name, value) -> builder.header(name, value.toString()));
        return JSONUtil.parseObj(CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofString()).body());
    }

    /** 错误必须明确且不触达下游。 */
    private void assertRejected(JSONObject wire, String message) throws Exception {
        DOWNSTREAM_URI.set(null);
        JSONObject result = sendWire(wire);
        assertThat(result.getBool("success")).isFalse();
        assertThat(result.toString()).contains(message);
        assertThat(DOWNSTREAM_URI.get()).isNull();
    }

    /** 浏览器生成 SM2 密文供 Hutool 解密，并反向验证服务端 SM2/SM4 密文。 */
    @Test
    void interoperatesWithActualBrowserAdaptersInBothDirections() throws Exception {
        JSONObject browser = node(Map.of("op", "vector", "publicKey", CRYPTO.getSm2PublicKey()));
        assertThat(CRYPTO.decryptSm4Key(browser.getStr("wrapped"))).isEqualTo(browser.getStr("key"));
        assertThat(CRYPTO.decrypt(browser.getStr("cipher"), browser.getStr("key"))).isEqualTo("你好 Macula / % 100");
        String cipher = CRYPTO.encrypt("服务端响应 中文 + &", browser.getStr("key"));
        assertThat(node(Map.of("op", "decrypt", "cipher", cipher, "key", browser.getStr("key"))).getStr("plain"))
            .isEqualTo("服务端响应 中文 + &");
        var sm2 = cn.hutool.crypto.SmUtil.sm2(null, browser.getStr("publicKey"));
        String wrapped = sm2.encryptHex(browser.getStr("key"), cn.hutool.crypto.asymmetric.KeyType.PublicKey);
        assertThat(node(Map.of("op", "unwrap", "wrapped", wrapped, "privateKey", browser.getStr("privateKey")))
            .getStr("key")).isEqualTo(browser.getStr("key"));
    }

    /** 调用内置端点并读取统一 JSON 响应。 */
    private JSONObject get(String path) throws Exception {
        return JSONUtil.parseObj(CLIENT.send(HttpRequest.newBuilder(uri(path)).timeout(TIMEOUT).GET().build(),
            HttpResponse.BodyHandlers.ofString()).body());
    }

    /** 为隔离测试构造本机临时网关地址。 */
    private URI uri(String path) {
        return URI.create("http://127.0.0.1:" + gateway.port() + path);
    }

    /** Node 加载实际生产 JS；缺少 Node 或协议失败时显式失败而非跳过。 */
    private JSONObject node(Map<String, Object> input) throws Exception {
        String script = """
            const fs = require('node:fs');
            const c = require('./src/main/resources/static/crypto-demo/crypto-client.js');
            const {createRequest} = require('./src/main/resources/static/crypto-demo/request.js');
            const sm2 = require('./src/main/resources/static/crypto-demo/vendor/sm2.js');
            const x = JSON.parse(fs.readFileSync(0, 'utf8'));
            (async () => {
                let out;
                if (x.op === 'request') {
                    const trace = [];
                    const request = createRequest({baseURL: x.baseUrl, getToken: () => x.token || '',
                        onTrace: event => trace.push(event)});
                    const url = '/consumer/api/v1/consumer';
                    const result = x.kind === 'plain' ? await request.get(url + '/plain', {params: {str: x.input.str}}) : x.kind === 'get'
                        ? await request.get(url + '/echo/' + encodeURIComponent(x.input.name), {params: {str: x.input.str}})
                        : await request.post(url + '/user', x.input);
                    out = {result, trace};
                }
                if (x.op === 'wire') {
                    const request = createRequest({baseURL: x.baseUrl, adapter: async config => {
                        const u = new URL(config.url);
                        if (u.pathname.startsWith('/gateway/')) {
                            const response = await fetch(u);
                            return {config, status: response.status, headers: {}, data: await response.text()};
                        }
                        out = {url: config.url, method: config.method.toUpperCase(), body: config.data, headers: config.headers.toJSON()};
                        return {config, status: 200, headers: {}, data: JSON.stringify({success:true, code:'00000', data:null})};
                    }});
                    if (x.kind === 'post') await request.post('/consumer/api/v1/consumer/user', x.input);
                    else await request.get('/consumer/api/v1/consumer/echo/demo', {params:x.input});
                }
                if (x.op === 'decrypt') out = {plain: c.decrypt(x.cipher, x.key)};
                if (x.op === 'unwrap') out = {key: sm2.doDecrypt(x.wrapped.slice(2), x.privateKey, 1)};
                if (x.op === 'vector') {
                    const key = c.newKey();
                    const pair = sm2.generateKeyPairHex();
                    const vector = require('./src/test/resources/crypto-demo/interop.json');
                    out = {...pair, key, wrapped: c.wrapKey(key, x.publicKey), cipher: c.encrypt(vector.plain, key)};
                }
                process.stdout.write(JSON.stringify(out));
            })().catch(error => { console.error(error.message); process.exitCode = 1; });
            """;
        Path output = Files.createTempFile("crypto-demo-node-", ".json");
        Path errors = Files.createTempFile("crypto-demo-node-", ".err");
        try {
            Process process = new ProcessBuilder("node", "-e", script).directory(Path.of("").toAbsolutePath().toFile())
                .redirectOutput(output.toFile()).redirectError(errors.toFile()).start();
            process.getOutputStream().write(JSONUtil.toJsonStr(input).getBytes(StandardCharsets.UTF_8));
            process.getOutputStream().close();
            if (!process.waitFor(20, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IllegalStateException("Node 协议互通验证超时");
            }
            assertThat(process.exitValue()).as("Node 协议适配：%s", Files.readString(errors)).isZero();
            return JSONUtil.parseObj(Files.readString(output));
        } finally {
            Files.deleteIfExists(output);
            Files.deleteIfExists(errors);
        }
    }
}
