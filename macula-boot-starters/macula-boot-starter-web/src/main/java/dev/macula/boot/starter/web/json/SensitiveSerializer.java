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

package dev.macula.boot.starter.web.json;

import cn.hutool.core.util.ObjectUtil;
import dev.macula.boot.starter.web.annotation.Sensitive;
import dev.macula.boot.starter.web.utils.SensitiveUtil;

import java.util.Objects;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * {@code SensitiveSerializer} 脱敏JSON序列号
 *
 * @author rain
 * @since 2022/7/28 23:53
 */
public class SensitiveSerializer extends ValueSerializer<String> {

    private Sensitive.Type type;
    private int startInclude;
    private int endExclude;
    private int overlayRepeat;

    public SensitiveSerializer() {

    }

    public SensitiveSerializer(final Sensitive sensitive) {
        this.type = sensitive.value();
        this.startInclude = sensitive.startInclude();
        this.endExclude = sensitive.endExclude();
        this.overlayRepeat = sensitive.overlayRepeat();
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializationContext serializers) throws JacksonException {
        if (ObjectUtil.isEmpty(value)) {
            gen.writeString(value);
            return;
        } else {
            switch (this.type) {
                case MOBILE:
                    gen.writeString(SensitiveUtil.handlerMobile(value));
                    break;
                case ID_CARD:
                    gen.writeString(SensitiveUtil.handlerIdCard(value));
                    break;
                case BANK_CARD:
                    gen.writeString(SensitiveUtil.handlerBankCard(value));
                    break;
                case CHINESE_NAME:
                    gen.writeString(SensitiveUtil.handlerUsername(value));
                    break;
                case FIXED_PHONE:
                    gen.writeString(SensitiveUtil.handlerFixedPhone(value));
                    break;
                case ADDRESS:
                    gen.writeString(SensitiveUtil.handlerAddress(value));
                    break;
                case EMAIL:
                    gen.writeString(SensitiveUtil.handlerEmail(value));
                    break;
                case CUSTOM_HIDE:
                    gen.writeString(SensitiveUtil.hide(value, startInclude, endExclude));
                    break;
                case CUSTOM_RETAIN_HIDE:
                    gen.writeString(SensitiveUtil.hide(value, startInclude, (value.length() - endExclude)));
                    break;
                case CUSTOM_OVERLAY:
                    gen.writeString(
                        SensitiveUtil.overlay(value, SensitiveUtil.ASTERISK, overlayRepeat, startInclude, endExclude));
                    break;
                default:
                    gen.writeString(value);
            }
        }

    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext prov, BeanProperty property) {
        if (Objects.isNull(property)) {
            return prov.getDefaultNullValueSerializer();
        }
        if (Objects.equals(property.getType().getRawClass(), String.class)) {
            Sensitive sensitive = property.getAnnotation(Sensitive.class);
            if (Objects.isNull(sensitive)) {
                sensitive = property.getContextAnnotation(Sensitive.class);
            }
            if (Objects.nonNull(sensitive)) {
                return new SensitiveSerializer(sensitive);
            }
        }
        return prov.findPrimaryPropertySerializer(property.getType(), property);
    }
}
