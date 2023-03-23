/*
 * Copyright (c) 2023 Macula
 *   macula.dev, China
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

package dev.macula.boot.starter.cloud.gateway.filter;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.net.URLEncoder;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import dev.macula.boot.result.ResultCode;
import dev.macula.boot.starter.cloud.gateway.config.GatewayProperties;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.crypto.CryptoService;
import jodd.util.StringUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * {@code ProcessCryptoReqResFilter} 加密或解密请求响应处理拦截器
 *
 * @author rain
 * @since 2023/3/22 23:17
 */
@Slf4j
@RefreshScope
@RequiredArgsConstructor
public class ProcessCryptoReqResFilter implements GlobalFilter, Ordered {

    private final CryptoService cryptoService;
    private final GatewayProperties properties;
    private URLEncoder urlEncoder = URLEncoder.createQuery();

    @SneakyThrows
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 添加是否加密标志
        exchange.getResponse().getHeaders()
            .add(GatewayConstants.CRYPTO_SWITCH, String.valueOf(properties.isCryptoSwitch()));

        // 判断是否有加密参数，有则进行加解密操作，无则跳过
        String path = exchange.getRequest().getURI().getPath();
        List<String> sm4Keys = exchange.getRequest().getHeaders().get(GatewayConstants.SM4_KEY);
        if (CollectionUtil.isEmpty(sm4Keys) || StrUtil.isBlank(sm4Keys.get(0))) {
            // 如果开启了接口强制加解密则需要并且URL是属于需要加解密的，则返回KEY为空的错误
            PathMatcher pathMatcher = new AntPathMatcher();
            if (properties.isCryptoSwitch() && properties.isForceCrypto() && properties.getCryptoUrls().stream()
                .anyMatch(s -> pathMatcher.match(s, path))) {

                return setFailedResponse(exchange, ApiResultCode.API_CRYPTO_KEY_NOT_EXIST,
                    "接口需要加密传输但是缺少KEY: PATH=" + path);
            }
            return chain.filter(exchange);
        }

        String sm4Key = sm4Keys.get(0);
        if (StrUtil.isNotBlank(sm4Key)) {
            try {
                // 解密密钥
                sm4Key = cryptoService.decryptSm4Key(sm4Key);
                log.debug("ProcessCryptoReqResFilter， sm4Key:{}", sm4Key);
                // 解密请求的内容
                exchange = processRequest(exchange, sm4Key);
            } catch (Exception e) {
                // 返回异常信息
                return setFailedResponse(exchange, ApiResultCode.API_CRYPTO_ERROR, e.getMessage());
            }
        }
        return chain.filter(exchange);
    }

    private Mono<Void> setFailedResponse(ServerWebExchange exchange, ResultCode code, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.setStatusCode(HttpStatus.OK);

        log.error("[加解密异常处理]请求路径:{}, 错误信息:{}", exchange.getRequest().getPath(), msg);

        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            String failedJson = JSONUtil.toJsonStr(Result.failed(code, msg));
            return bufferFactory.wrap(failedJson.getBytes(StandardCharsets.UTF_8));
        }));
    }

    private ServerWebExchange processRequest(ServerWebExchange exchange, String sm4Key) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String method = serverHttpRequest.getMethodValue();
        ServerHttpRequest newRequest = exchange.getRequest();
        // 解密请求内容
        if ("POST".equals(method) || "PUT".equals(method)) {
            newRequest = processPostRequest(exchange, sm4Key);
        } else if ("GET".equals(method)) {
            newRequest = processGetRequest(exchange, sm4Key);
        }

        // 加密响应内容
        ServerHttpResponseDecorator serverHttpResponseDecorator = processResponse(exchange, sm4Key);

        return exchange.mutate().request(newRequest).response(serverHttpResponseDecorator).build();
    }

    private ServerHttpRequest processGetRequest(ServerWebExchange exchange, String sm4Key) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String path = serverHttpRequest.getURI().getPath();
        log.debug("======editGetParam start path:{}", path);
        MultiValueMap<String, String> queryParams = serverHttpRequest.getQueryParams();
        // GET请求的加密参数附加在URL?data=xxx中
        List<String> data = queryParams.get(GatewayConstants.CRYPTO_DATA_KEY);
        if (CollectionUtils.isEmpty(data)) {
            return serverHttpRequest;
        }

        String encrtyParam = data.get(0);
        log.debug("======editGetParam start path:{}, encrtyParam: {}", path, encrtyParam);

        // 解密URL参数
        String decrypt = cryptoService.decrypt(encrtyParam, sm4Key);

        if (StrUtil.isNotBlank(decrypt)) {
            // 去除字符串中的引号
            String replace = decrypt.replace("\"", "");
            URI uri = serverHttpRequest.getURI();
            String uriStr = uri.toString();
            if (StringUtil.isNotBlank(uriStr) && StrUtil.isNotBlank(replace)) {
                String[] split = uriStr.split("\\?");
                String encodePath = urlEncoder.encode(replace, CharsetUtil.CHARSET_UTF_8);
                String newPath = new StringBuilder(split[0]).append("?").append(encodePath).toString();
                log.debug("======editGetParam new path:{}", newPath);
                URI newUri = uri.resolve(newPath);
                ServerHttpRequest request = serverHttpRequest.mutate().uri(newUri).build();
                return request;
            }
        }
        return serverHttpRequest;
    }

    private ServerHttpRequest processPostRequest(ServerWebExchange exchange, String sm4Key) {
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        // 尝试从 exchange 的自定义属性中取出缓存到的 body
        Object cachedRequestBodyObject =
            exchange.getAttributeOrDefault(GatewayConstants.CACHED_REQUEST_BODY_OBJECT_KEY, null);

        if (cachedRequestBodyObject != null) {
            byte[] decrypBytes;
            try {
                byte[] body = (byte[])cachedRequestBodyObject;
                String rootData = new String(body, StandardCharsets.UTF_8);
                decrypBytes = body;
                JSONObject jsonObject = JSONUtil.parseObj(rootData);
                Object data = null;
                if (jsonObject != null) {
                    data = jsonObject.get(GatewayConstants.CRYPTO_DATA_KEY);
                }
                if (data != null) {
                    String enStr = (String)data;
                    if (StrUtil.isNotBlank(enStr)) {
                        // 解密请求内容
                        String deJson = cryptoService.decrypt(enStr, sm4Key);
                        if (StrUtil.isNotBlank(deJson)) {
                            log.debug("最终的JSON数据为:{}", deJson);
                            decrypBytes = deJson.getBytes(StandardCharsets.UTF_8);
                        }
                    }
                }
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            // 根据解密后的参数重新构建请求体
            DataBufferFactory dataBufferFactory = exchange.getResponse().bufferFactory();
            Flux<DataBuffer> bodyFlux = Flux.just(dataBufferFactory.wrap(decrypBytes));

            // 构建新的请求头
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(exchange.getRequest().getHeaders());
            // 由于修改了传递参数，需要重新设置CONTENT_LENGTH，长度是字节长度，不是字符串长度
            int length = decrypBytes.length;
            headers.remove(HttpHeaders.CONTENT_LENGTH);
            headers.setContentLength(length);

            ServerHttpRequest newRequest = request.mutate().uri(uri).build();
            newRequest = new ServerHttpRequestDecorator(newRequest) {
                @Override
                public Flux<DataBuffer> getBody() {
                    return bodyFlux;
                }

                @Override
                public HttpHeaders getHeaders() {
                    return headers;
                }
            };

            return newRequest;
        }
        return request;
    }

    private ServerHttpResponseDecorator processResponse(ServerWebExchange exchange, String sm4Key) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = (Flux<? extends DataBuffer>)body;
                    return super.writeWith(fluxBody.buffer().map(dataBuffer -> {
                        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                        DataBuffer join = dataBufferFactory.join(dataBuffer);

                        byte[] contentByte = new byte[join.readableByteCount()];
                        join.read(contentByte);

                        //释放掉内存
                        DataBufferUtils.release(join);
                        // 正常返回的数据
                        String result = new String(contentByte, StandardCharsets.UTF_8);
                        JSONObject node = JSONUtil.parseObj(result);
                        Object data = node.get(GatewayConstants.CRYPTO_DATA_KEY);
                        log.debug("======processResponse body:{}, path:{}", data, path);
                        if (data != null) {
                            String text = data.toString();
                            String content = null;
                            try {
                                content = cryptoService.encrypt(text, sm4Key);
                            } catch (Exception e) {
                                throw new IllegalStateException("响应数据加密异常：" + e.getMessage());
                            }

                            node.set(GatewayConstants.CRYPTO_DATA_KEY, content);
                            log.debug("修改响应体,修改前:{},修改后:{}, path:{}", text, node.toStringPretty(), path);
                            // 设置body长度
                            byte[] resultBytes = node.toJSONString(2).getBytes(StandardCharsets.UTF_8);
                            originalResponse.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
                            originalResponse.getHeaders().setContentLength(resultBytes.length);

                            return bufferFactory.wrap(resultBytes);
                        } else {
                            log.info("data 为空，不修改响应体,响应参数:{}, path:{}", data, path);
                            return bufferFactory.wrap(result.getBytes(StandardCharsets.UTF_8));
                        }
                    }));
                }
                return super.writeWith(body);
            }
        };
        return decoratedResponse;
    }

    @Override
    public int getOrder() {
        return -50;
    }
}