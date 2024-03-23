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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 针对 api 服务对 android 和 ios 和 web 处理的 分读写的 jackson 处理
 *
 * <p>
 * 1. app 端上报数据是 使用 readObjectMapper
 * 2. 返回给 app 端的数据使用 writeObjectMapper
 * </p>
 *
 * @author L.cm, Rain
 */
public class MappingApiJackson2HttpMessageConverter extends AbstractReadWriteJackson2HttpMessageConverter {

    @Nullable
    private String jsonPrefix;

    /**
     * Construct a new {@link MappingApiJackson2HttpMessageConverter} with a custom {@link ObjectMapper}.
     * You can use {@link Jackson2ObjectMapperBuilder} to build it easily.
     *
     * @param objectMapper ObjectMapper
     * @see Jackson2ObjectMapperBuilder#json()
     */
    public MappingApiJackson2HttpMessageConverter(ObjectMapper objectMapper) {
        super(objectMapper, initWriteObjectMapper(objectMapper), initMediaType());
    }

    private static List<MediaType> initMediaType() {
        List<MediaType> supportedMediaTypes = new ArrayList<>();
        supportedMediaTypes.add(MediaType.APPLICATION_JSON);
        supportedMediaTypes.add(new MediaType("application", "*+json"));
        return supportedMediaTypes;
    }

    private static ObjectMapper initWriteObjectMapper(ObjectMapper readObjectMapper) {
        // 拷贝 readObjectMapper
        ObjectMapper writeObjectMapper = readObjectMapper.copy();
        // null 处理
        writeObjectMapper.setSerializerFactory(writeObjectMapper.getSerializerFactory().withSerializerModifier(new MaculaBeanSerializerModifier()));
        writeObjectMapper.getSerializerProvider().setNullValueSerializer(MaculaBeanSerializerModifier.NullJsonSerializers.STRING_JSON_SERIALIZER);
        return writeObjectMapper;
    }

    /**
     * Specify a custom prefix to use for this view's JSON output.
     * Default is none.
     *
     * @param jsonPrefix jsonPrefix
     * @see #setPrefixJson
     */
    public void setJsonPrefix(String jsonPrefix) {
        this.jsonPrefix = jsonPrefix;
    }

    /**
     * Indicate whether the JSON output by this view should be prefixed with ")]}', ". Default is false.
     * <p>Prefixing the JSON string in this manner is used to help prevent JSON Hijacking.
     * The prefix renders the string syntactically invalid as a script so that it cannot be hijacked.
     * This prefix should be stripped before parsing the string as JSON.
     *
     * @param prefixJson prefixJson
     * @see #setJsonPrefix
     */
    public void setPrefixJson(boolean prefixJson) {
        this.jsonPrefix = (prefixJson ? ")]}', " : null);
    }

    @Override
    protected void writePrefix(JsonGenerator generator, Object object) throws IOException {
        if (this.jsonPrefix != null) {
            generator.writeRaw(this.jsonPrefix);
        }
    }
}
