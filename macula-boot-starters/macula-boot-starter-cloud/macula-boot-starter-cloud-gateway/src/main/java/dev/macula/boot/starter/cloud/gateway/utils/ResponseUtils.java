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

package dev.macula.boot.starter.cloud.gateway.utils;

import cn.hutool.json.JSONUtil;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * <p>
 * <b>ResponseUtils</b> 响应助手
 * </p>
 *
 * @author Rain
 * @since 2022-02-20
 */
public class ResponseUtils {

    public static Mono<Void> writeErrorInfo(ServerHttpResponse response, ApiResultCode resultCode) {
        return writeErrorInfo(response, resultCode, null);
    }

    /**
     * 以非HTTP 200响应错误信息
     *
     * @param response 响应
     * @param code     错误码
     * @param result   具体错误结果
     * @return void
     */
    public static Mono<Void> writeErrorInfo(ServerHttpResponse response, ApiResultCode code, Result<?> result) {
        switch (code) {
            case ACCESS_UNAUTHORIZED:
            case TOKEN_INVALID_OR_EXPIRED:
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                break;
            case AKSK_ACCESS_FORBIDDEN:
            case TOKEN_ACCESS_FORBIDDEN:
                response.setStatusCode(HttpStatus.FORBIDDEN);
                break;
            default:
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                break;
        }
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().setAccessControlAllowOrigin("*");
        response.getHeaders().setCacheControl(CacheControl.noCache());

        String body = JSONUtil.toJsonStr(result == null ? Result.failed(code) : result);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer)).doOnError(error -> DataBufferUtils.release(buffer));
    }

    /**
     * 以 HTTP 200返回错误信息
     *
     * @param response 响应
     * @param code     错误码
     * @param msg      错误消息
     * @return void
     */
    public static Mono<Void> writeOkErrorInfo(ServerHttpResponse response, ApiResultCode code, String msg) {
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().setAccessControlAllowOrigin("*");
        response.getHeaders().setCacheControl(CacheControl.noCache());
        response.setStatusCode(HttpStatus.OK);

        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            String failedJson = JSONUtil.toJsonStr(Result.failed(code, msg));
            return bufferFactory.wrap(failedJson.getBytes(StandardCharsets.UTF_8));
        }));
    }
}