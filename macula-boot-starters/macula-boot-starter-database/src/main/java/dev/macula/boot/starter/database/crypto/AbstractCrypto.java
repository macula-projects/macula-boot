/*
 * Copyright (c) 2022 Macula
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

package dev.macula.boot.starter.database.crypto;

import cn.hutool.crypto.symmetric.AES;
import dev.macula.boot.starter.database.config.MyBatisPlusProperties;

/**
 * {@code AbstractCrypto} 加解密基类
 *
 * @author rain
 * @since 2022/8/23 14:29
 */
public abstract class AbstractCrypto {
    private MyBatisPlusProperties properties;
    private AES aes;

    public AbstractCrypto(MyBatisPlusProperties properties) {
        this.properties = properties;
    }

    protected AES getAES() {
        if (aes == null) {
            aes = new AES(properties.getAes().getMode(),
                    properties.getAes().getPadding(),
                    properties.getAes().getKey().getBytes(),
                    properties.getAes().getIv() == null ? null : properties.getAes().getIv().getBytes());
        }
        return aes;
    }
}
