/*
 * Copyright (c) 2024 Macula
 *    macula.dev, China
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package dev.macula.boot.starter.web.json;

import org.springframework.core.ResolvableType;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractJacksonHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.StreamUtils;
import org.springframework.util.TypeUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.ser.FilterProvider;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 分读写的 JSON 消息处理器。
 *
 * <p>保留历史类名以兼容已有调用方，内部实现使用 Spring Framework 7 和 Jackson 3。</p>
 *
 * @author L.cm
 * @since 5.0.0
 */
public abstract class AbstractReadWriteJackson2HttpMessageConverter
    extends AbstractJacksonHttpMessageConverter<ObjectMapper> {

    private static final String JSON_VIEW_HINT = "com.fasterxml.jackson.annotation.JsonView";
    private static final String FILTER_PROVIDER_HINT = FilterProvider.class.getName();

    private final ObjectMapper writeObjectMapper;

    public AbstractReadWriteJackson2HttpMessageConverter(ObjectMapper readObjectMapper,
        ObjectMapper writeObjectMapper) {
        super(readObjectMapper);
        this.writeObjectMapper = writeObjectMapper;
    }

    public AbstractReadWriteJackson2HttpMessageConverter(ObjectMapper readObjectMapper,
        ObjectMapper writeObjectMapper, MediaType supportedMediaType) {
        super(readObjectMapper, supportedMediaType);
        this.writeObjectMapper = writeObjectMapper;
    }

    public AbstractReadWriteJackson2HttpMessageConverter(ObjectMapper readObjectMapper,
        ObjectMapper writeObjectMapper, List<MediaType> supportedMediaTypes) {
        super(readObjectMapper, supportedMediaTypes.toArray(MediaType[]::new));
        this.writeObjectMapper = writeObjectMapper;
    }

    @Override
    protected void writeInternal(Object object, ResolvableType resolvableType, HttpOutputMessage outputMessage,
        @Nullable Map<String, Object> hints) throws IOException, HttpMessageNotWritableException {
        MediaType contentType = outputMessage.getHeaders().getContentType();
        JsonEncoding encoding = getJsonEncoding(contentType);
        OutputStream outputStream = StreamUtils.nonClosing(outputMessage.getBody());

        Class<?> serializationView = null;
        FilterProvider filters = null;
        JavaType javaType = null;
        Type type = resolvableType.getType();
        if (TypeUtils.isAssignable(type, object.getClass())) {
            javaType = getJavaType(type, null);
        }
        if (hints != null) {
            serializationView = (Class<?>)hints.get(JSON_VIEW_HINT);
            filters = (FilterProvider)hints.get(FILTER_PROVIDER_HINT);
        }

        ObjectWriter objectWriter = serializationView != null ?
            writeObjectMapper.writerWithView(serializationView) : writeObjectMapper.writer();
        if (filters != null) {
            objectWriter = objectWriter.with(filters);
        }
        if (javaType != null && (javaType.isContainerType() || javaType.isTypeOrSubTypeOf(Optional.class))) {
            objectWriter = objectWriter.forType(javaType);
        }
        SerializationConfig config = objectWriter.getConfig();
        if (contentType != null && contentType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM)
            && config.isEnabled(SerializationFeature.INDENT_OUTPUT)) {
            objectWriter = objectWriter.withDefaultPrettyPrinter();
        }

        try (JsonGenerator generator = objectWriter.createGenerator(outputStream, encoding)) {
            writePrefix(generator, object);
            objectWriter.writeValue(generator, object);
            writeSuffix(generator, object);
            generator.flush();
        } catch (InvalidDefinitionException ex) {
            throw new HttpMessageConversionException("Type definition error: " + ex.getType(), ex);
        } catch (JacksonException ex) {
            throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getOriginalMessage(), ex);
        }
    }
}
