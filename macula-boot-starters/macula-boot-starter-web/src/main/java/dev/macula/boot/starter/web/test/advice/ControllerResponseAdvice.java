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

package dev.macula.boot.starter.web.test.advice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.exception.BizException;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import dev.macula.boot.starter.web.test.annotation.NotControllerResponseAdvice;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * {@code ControllerResponseAdvice} 对于非Result的返回值进行包装
 *
 * @author rain
 * @since 2022/6/29 16:10
 */
@RestControllerAdvice
public class ControllerResponseAdvice implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        // response是ResultVo类型，或者注释了NotControllerResponseAdvice都不进行包装
        return !(methodParameter.getParameterType().isAssignableFrom(Result.class) || methodParameter.getParameterType()
            .isAssignableFrom(ResponseEntity.class) || methodParameter.hasMethodAnnotation(
            NotControllerResponseAdvice.class));
    }

    @Override
    public Object beforeBodyWrite(Object data, MethodParameter returnType, MediaType mediaType,
        Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest request, ServerHttpResponse response) {
        if (request.getURI().getPath().contains("v3/api-docs")) {
            return data;
        }

        if (request.getURI().getPath().contains("actuator")) {
            return data;
        }

        // Feign请求时通过拦截器设置请求头，如果是Feign请求则直接返回实体对象
        if (request.getHeaders().containsKey(GlobalConstants.FEIGN_REQ_ID)) {
            return data;
        }

        // String类型不能直接包装
        if (returnType.getGenericParameterType().equals(String.class)) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                // 将数据包装在ResultVo里后转换为json串进行返回
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return objectMapper.writeValueAsString(Result.success(data));
            } catch (JsonProcessingException e) {
                throw new BizException(ApiResultCode.RESPONSE_PACK_ERROR, e.getMessage());
            }
        }
        // 否则直接包装成ResultVo返回
        return Result.success(data);
    }
}
