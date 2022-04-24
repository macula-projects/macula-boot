package org.macula.boot.starter.gateway.utils;

import cn.hutool.json.JSONUtil;
import org.macula.boot.starter.api.ApiResultCode;
import org.macula.boot.starter.api.Result;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
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
        switch (resultCode) {
            case ACCESS_UNAUTHORIZED:
            case TOKEN_INVALID_OR_EXPIRED:
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                break;
            case TOKEN_ACCESS_FORBIDDEN:
                response.setStatusCode(HttpStatus.FORBIDDEN);
                break;
            default:
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                break;
        }
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        response.getHeaders().set("Access-Control-Allow-Origin", "*");
        response.getHeaders().set("Cache-Control", "no-cache");
        String body = JSONUtil.toJsonStr(Result.failed(resultCode));
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer))
            .doOnError(error -> DataBufferUtils.release(buffer));
    }

}