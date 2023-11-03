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
import dev.macula.boot.result.Result;
import org.springframework.core.io.buffer.DataBuffer;
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

    public static Mono<Void> writeResult(ServerHttpResponse response, Result<?> result) {
        return writeResult(response, HttpStatus.OK, result);
    }

    /**
     * 输出指定Status的响应
     *
     * @param response 响应
     * @param status   状态码
     * @param result   响应内容
     * @return void
     */
    public static Mono<Void> writeResult(ServerHttpResponse response, HttpStatus status, Result<?> result) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().setAccessControlAllowOrigin("*");
        response.getHeaders().setCacheControl(CacheControl.noCache());

        String json = JSONUtil.toJsonStr(result);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        response.getHeaders().setContentLength(bytes.length);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}