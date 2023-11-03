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

import cn.hutool.core.codec.PercentCodec;
import cn.hutool.core.net.RFC3986;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import dev.macula.boot.starter.cloud.gateway.config.GatewayProperties;
import dev.macula.boot.starter.cloud.gateway.constants.GatewayConstants;
import dev.macula.boot.starter.cloud.gateway.crypto.CryptoService;
import dev.macula.boot.starter.cloud.gateway.utils.RequestUtils;
import dev.macula.boot.starter.cloud.gateway.utils.ResponseUtils;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
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
import java.util.Objects;

/**
 * {@code CryptoGlobalFilter} 加密或解密请求响应处理拦截器
 * TODO 目前只支持application/json，需要研究支持form-data、url-encoded等
 *
 * @author rain
 * @since 2023/3/22 23:17
 */
@Slf4j
@RefreshScope
@RequiredArgsConstructor
public class CryptoGlobalFilter implements GlobalFilter, Ordered {

    private final CryptoService cryptoService;
    private final GatewayProperties properties;
    private final PercentCodec urlEncoder = RFC3986.QUERY;

    @SneakyThrows
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        // 添加是否加密标志
        response.getHeaders().add(GatewayConstants.CRYPTO_SWITCH, String.valueOf(properties.isCryptoSwitch()));

        String path = RequestUtils.getOriginPath(exchange);

        // 判断是否有加密参数，有则进行加解密操作，无则跳过
        String sm4Key = request.getHeaders().getFirst(GatewayConstants.SM4_KEY);
        String symAlg = request.getHeaders().getFirst((GatewayConstants.SYM_ALG));
        if (StrUtil.isEmpty(sm4Key) || StrUtil.isEmpty(symAlg)) {
            // 如果开启了接口强制加解密则需要并且URL是属于需要加解密的，则返回KEY为空的错误
            PathMatcher pathMatcher = new AntPathMatcher();
            if (properties.isCryptoSwitch() && properties.isForceCrypto() && properties.getProtectUrls().getCrypto()
                .stream().anyMatch(s -> pathMatcher.match(s, path))) {

                return ResponseUtils.writeResult(response,
                    Result.failed(ApiResultCode.API_CRYPTO_KEY_NOT_EXIST, "接口需要加密传输但是缺少KEY: PATH=" + path));
            }
            return chain.filter(exchange);
        }

        try {
            // 解密密钥
            sm4Key = cryptoService.decryptSm4Key(sm4Key);
            log.debug("ProcessCryptoReqResFilter， sm4Key:{}", sm4Key);
            // 解密请求的内容
            exchange = processRequest(exchange, sm4Key);
        } catch (Exception e) {
            // 返回异常信息
            return ResponseUtils.writeResult(response, Result.failed(ApiResultCode.API_CRYPTO_ERROR, e.getMessage()));
        }
        return chain.filter(exchange);
    }

    private ServerWebExchange processRequest(ServerWebExchange exchange, String sm4Key) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        HttpMethod method = serverHttpRequest.getMethod();
        ServerHttpRequest newRequest = exchange.getRequest();
        // 解密请求内容
        if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            newRequest = processPostRequest(exchange, sm4Key);
        } else if (method == HttpMethod.GET || method == HttpMethod.DELETE) {
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

        String encryptParam = data.get(0);
        log.debug("======editGetParam start path:{}, encrtyParam: {}", path, encryptParam);

        // 解密URL参数
        String decrypt = cryptoService.decrypt(encryptParam, sm4Key);

        if (StrUtil.isNotBlank(decrypt)) {
            // 去除字符串中的引号
            String replace = decrypt.replace("\"", "");
            URI uri = serverHttpRequest.getURI();
            String uriStr = uri.toString();
            if (StringUtil.isNotBlank(uriStr) && StrUtil.isNotBlank(replace)) {
                String[] split = uriStr.split("\\?");
                String encodePath = urlEncoder.encode(replace, CharsetUtil.CHARSET_UTF_8);
                String newPath = split[0] + "?" + encodePath;
                log.debug("======editGetParam new path:{}", newPath);
                URI newUri = uri.resolve(newPath);
                return serverHttpRequest.mutate().uri(newUri).build();
            }
        }
        return serverHttpRequest;
    }

    private ServerHttpRequest processPostRequest(ServerWebExchange exchange, String sm4Key) {
        ServerHttpRequest request = exchange.getRequest();
        // 尝试从 exchange 的自定义属性中取出缓存到的 body
        byte[] body = RequestUtils.getBody(exchange);
        if (body != null && body.length > 0 && Objects.equals(request.getHeaders().getContentType(),
            MediaType.APPLICATION_JSON)) {
            byte[] decrypBytes = body;

            String rootData = new String(body, StandardCharsets.UTF_8);
            JSONObject jsonObject = JSONUtil.parseObj(rootData);
            if (jsonObject != null) {
                Object data = jsonObject.get(GatewayConstants.CRYPTO_DATA_KEY);
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
            }
            // 根据解密后的参数重新构建请求体
            return RequestUtils.rewriteRequestBody(exchange, decrypBytes);
        }
        return request;
    }

    private ServerHttpResponseDecorator processResponse(ServerWebExchange exchange, String sm4Key) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        return new ServerHttpResponseDecorator(originalResponse) {
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
    }

    @Override
    public int getOrder() {
        return -5;
    }
}