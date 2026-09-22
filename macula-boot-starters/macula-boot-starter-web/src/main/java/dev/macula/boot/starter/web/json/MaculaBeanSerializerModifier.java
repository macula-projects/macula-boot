/*
 * Copyright (c) 2024 Macula
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

package dev.macula.boot.starter.web.json;

import java.time.OffsetDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.ValueSerializerModifier;

/**
 * {@code MaculaBeanSerializerModifier} jackson 默认值为 null 时的处理，主要是为了避免 app 端出现null导致闪退
 * <p>
 * 规则：
 * <ul>
 *  <li>number -1 </li>
 *  <li>string "" </li>
 *  <li>date "" </li>
 *  <li>boolean false </li>
 *  <li>array [] </li>
 *  <li>Object {} </li>
 * </ul>
 *
 * @author L.cm, Rain
 * @since 2024/3/14 21:09
 */
public class MaculaBeanSerializerModifier extends ValueSerializerModifier {
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription.Supplier beanDesc,
        List<BeanPropertyWriter> beanProperties) {
        // 循环所有的beanPropertyWriter
        beanProperties.forEach(writer -> {
            // 如果已经有 null 序列化处理如注解：@JsonSerialize(nullsUsing = xxx) 跳过
            if (writer.hasNullSerializer()) {
                return;
            }
            JavaType type = writer.getType();
            Class<?> clazz = type.getRawClass();
            if (type.isTypeOrSubTypeOf(Number.class)) {
                writer.assignNullSerializer(NullJsonSerializers.NUMBER_JSON_SERIALIZER);
            } else if (type.isTypeOrSubTypeOf(Boolean.class)) {
                writer.assignNullSerializer(NullJsonSerializers.BOOLEAN_JSON_SERIALIZER);
            } else if (type.isTypeOrSubTypeOf(Character.class)) {
                writer.assignNullSerializer(NullJsonSerializers.STRING_JSON_SERIALIZER);
            } else if (type.isTypeOrSubTypeOf(String.class)) {
                writer.assignNullSerializer(NullJsonSerializers.STRING_JSON_SERIALIZER);
            } else if (type.isArrayType() || clazz.isArray() || type.isTypeOrSubTypeOf(Collection.class)) {
                writer.assignNullSerializer(NullJsonSerializers.ARRAY_JSON_SERIALIZER);
            } else if (type.isTypeOrSubTypeOf(OffsetDateTime.class)) {
                writer.assignNullSerializer(NullJsonSerializers.STRING_JSON_SERIALIZER);
            } else if (type.isTypeOrSubTypeOf(Date.class) || type.isTypeOrSubTypeOf(TemporalAccessor.class)) {
                writer.assignNullSerializer(NullJsonSerializers.STRING_JSON_SERIALIZER);
            } else {
                writer.assignNullSerializer(NullJsonSerializers.OBJECT_JSON_SERIALIZER);
            }
        });
        return super.changeProperties(config, beanDesc, beanProperties);
    }

    /**
     * 各类空值的默认 JSON 序列化器集合。
     *
     * @since 5.0.0
     */
    public interface NullJsonSerializers {

        ValueSerializer<Object> STRING_JSON_SERIALIZER = new ValueSerializer<Object>() {
            @Override
            public void serialize(Object value, JsonGenerator gen, SerializationContext serializers)
                throws JacksonException {
                gen.writeString("");
            }
        };

        ValueSerializer<Object> NUMBER_JSON_SERIALIZER = new ValueSerializer<Object>() {
            @Override
            public void serialize(Object value, JsonGenerator gen, SerializationContext serializers)
                throws JacksonException {
                gen.writeNumber(-1);
            }
        };

        ValueSerializer<Object> BOOLEAN_JSON_SERIALIZER = new ValueSerializer<Object>() {
            @Override
            public void serialize(Object value, JsonGenerator gen, SerializationContext serializers)
                throws JacksonException {
                gen.writeBoolean(false);
            }
        };

        ValueSerializer<Object> ARRAY_JSON_SERIALIZER = new ValueSerializer<Object>() {
            @Override
            public void serialize(Object value, JsonGenerator gen, SerializationContext serializers)
                throws JacksonException {
                gen.writeStartArray();
                gen.writeEndArray();
            }
        };

        ValueSerializer<Object> OBJECT_JSON_SERIALIZER = new ValueSerializer<Object>() {
            @Override
            public void serialize(Object value, JsonGenerator gen, SerializationContext serializers)
                throws JacksonException {
                gen.writeStartObject();
                gen.writeEndObject();
            }
        };
    }
}
