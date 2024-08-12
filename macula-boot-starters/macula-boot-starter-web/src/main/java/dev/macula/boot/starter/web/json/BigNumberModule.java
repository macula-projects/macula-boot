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

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializerBase;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * <p>
 * <b>MaculaNumberModule</b> 处理Long、BigInteger、BigDecimal等有可能超出JS数值范围的序列化
 * </p>
 *
 * @author Rain
 * @since 2024/3/25
 */
public class BigNumberModule extends SimpleModule {
    public BigNumberModule(boolean longToString) {
        super(BigNumberModule.class.getName());
        if (longToString) {
            // BigDecimal的toString会生成科学计数法，前端识别不了
            this.addSerializer(BigDecimal.class, new ToStringSerializerBase(BigDecimal.class) {
                @Override
                public String valueToString(Object value) {
                    if (value instanceof BigDecimal) {
                        return ((BigDecimal) value).toPlainString();
                    }
                    return value.toString();
                }
            });
            this.addSerializer(BigInteger.class, ToStringSerializer.instance);
            this.addSerializer(Long.class, ToStringSerializer.instance);
            this.addSerializer(Long.TYPE, ToStringSerializer.instance);
        } else {
            this.addSerializer(BigDecimal.class, BigNumberSerializer.instance);
            this.addSerializer(BigInteger.class, BigNumberSerializer.instance);
            this.addSerializer(Long.class, BigNumberSerializer.instance);
            this.addSerializer(Long.TYPE, BigNumberSerializer.instance);
        }
    }
}
