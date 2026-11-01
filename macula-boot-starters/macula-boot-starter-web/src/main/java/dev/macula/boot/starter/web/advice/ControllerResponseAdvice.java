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

package dev.macula.boot.starter.web.advice;

import dev.macula.boot.constants.GlobalConstants;
import dev.macula.boot.exception.BizException;
import dev.macula.boot.result.ApiResultCode;
import dev.macula.boot.result.Result;
import dev.macula.boot.starter.web.annotation.NotControllerResponseAdvice;
import dev.macula.boot.starter.web.config.JacksonProperties;
import dev.macula.boot.security.PasskeyIssuer;
import dev.macula.boot.security.PasskeyResource;
import dev.macula.boot.security.PasskeyResources;
import dev.macula.boot.security.ResourceAccessException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import java.util.List;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.ObjectMapper;

/**
 * {@code ControllerResponseAdvice} 对于非Result的返回值进行包装
 *
 * @author rain
 * @since 2022/6/29 16:10
 */
@RestControllerAdvice
public class ControllerResponseAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;
    private final JacksonProperties jacksonProperties;
    private final ObjectProvider<PasskeyIssuer> issuers;
    private final boolean passkeyEnabled;

    /**
     * Preserves the original construction contract without a signing provider.
     * @param objectMapper JSON mapper
     * @param jacksonProperties JSON settings
     */
    public ControllerResponseAdvice(ObjectMapper objectMapper, JacksonProperties jacksonProperties) {
        this(objectMapper, jacksonProperties, null, false);
    }

    /**
     * Creates response processing with an optional security-provided issuer.
     * @param objectMapper JSON mapper
     * @param jacksonProperties JSON settings
     * @param issuers lazy issuer provider, null for the legacy constructor
     * @param passkeyEnabled whether list credentials should be issued
     */
    @Autowired
    public ControllerResponseAdvice(ObjectMapper objectMapper, JacksonProperties jacksonProperties,
        ObjectProvider<PasskeyIssuer> issuers,
        @Value("${macula.security.passkey.enabled:false}") boolean passkeyEnabled) {
        this.objectMapper = objectMapper;
        this.jacksonProperties = jacksonProperties;
        this.issuers = issuers;
        this.passkeyEnabled = passkeyEnabled;
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Class<? extends HttpMessageConverter<?>> aClass) {
        // Issuance is independent of the original wrapping opt-outs.
        return true;
    }

    private boolean shouldWrap(MethodParameter methodParameter) {
        return !(methodParameter.getParameterType().isAssignableFrom(Result.class) || methodParameter.getParameterType()
            .isAssignableFrom(ResponseEntity.class) || methodParameter.hasMethodAnnotation(
            NotControllerResponseAdvice.class));
    }

    @Override
    public Object beforeBodyWrite(Object data, MethodParameter returnType, MediaType mediaType,
        Class<? extends HttpMessageConverter<?>> aClass, ServerHttpRequest request, ServerHttpResponse response) {
        if (passkeyEnabled) {
            Object payload = data instanceof ResponseEntity<?> entity ? entity.getBody() : data;
            List<PasskeyResource> resources = PasskeyResources.find(payload);
            if (!resources.isEmpty()) {
                PasskeyIssuer issuer = issuers == null ? null : issuers.getIfAvailable();
                if (issuer == null) {
                    throw new ResourceAccessException("Resource credential issuer is unavailable");
                }
                for (PasskeyResource resource : resources) {
                    issuer.issue(resource);
                }
            }
        }
        if (!shouldWrap(returnType)) {
            return data;
        }

        if (request.getURI().getPath().contains("v3/api-docs")) {
            return data;
        }

        if (request.getURI().getPath().contains("actuator")) {
            return data;
        }

        // Feign请求时通过拦截器设置请求头，如果是Feign请求则直接返回实体对象
        if (request.getHeaders().containsHeader(GlobalConstants.FEIGN_REQ_ID)) {
            return data;
        }

        // String类型不能直接包装
        if (returnType.getGenericParameterType().equals(String.class)) {
            try {
                // 将数据包装在ResultVo里后转换为json串进行返回
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                // Flow Null To Empty的逻辑 @see MaculaBeanSerializerModifier
                if (data == null && jacksonProperties.isNullToEmpty()) {
                    data = JsonToken.START_OBJECT.asString() + JsonToken.END_OBJECT.asString();
                }
                return objectMapper.writeValueAsString(Result.success(data));
            } catch (JacksonException e) {
                throw new BizException(ApiResultCode.RESPONSE_PACK_ERROR, e.getMessage());
            }
        }
        // 否则直接包装成ResultVo返回
        return Result.success(data);
    }
}
