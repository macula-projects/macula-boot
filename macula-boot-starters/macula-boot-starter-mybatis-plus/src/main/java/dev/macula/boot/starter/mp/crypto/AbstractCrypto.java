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

package dev.macula.boot.starter.mp.crypto;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.symmetric.AES;
import dev.macula.boot.starter.mp.config.MyBatisPlusProperties;

/**
 * {@code AbstractCrypto} 加解密基类
 *
 * @author rain
 * @since 2022/8/23 14:29
 */
public abstract class AbstractCrypto {

    private final static String PREFIX = "ENC(";

    private final static String SUFFIX = ")";

    private MyBatisPlusProperties properties;
    private AES aes;

    public AbstractCrypto(MyBatisPlusProperties properties) {
        this.properties = properties;
    }

    private AES getAES() {
        if (aes == null) {
            aes = new AES(properties.getAes().getMode(), properties.getAes().getPadding(),
                properties.getAes().getKey().getBytes(),
                properties.getAes().getIv() == null ? null : properties.getAes().getIv().getBytes());
        }
        return aes;
    }

    protected String encryptBase64(String plaintext) {
        String cryptoText = getAES().encryptBase64(plaintext, CharsetUtil.UTF_8);
        cryptoText = StrUtil.addPrefixIfNot(cryptoText, PREFIX);
        return StrUtil.addSuffixIfNot(cryptoText, SUFFIX);
    }

    protected String decryptStr(String cryptoText) {
        // 如果是ENC(xxx)包裹的才做解密动作
        if (cryptoText.startsWith(PREFIX) && cryptoText.endsWith(SUFFIX)) {
            cryptoText = StrUtil.removePrefixIgnoreCase(cryptoText, PREFIX);
            cryptoText = StrUtil.removeSuffixIgnoreCase(cryptoText, SUFFIX);
            return getAES().decryptStr(cryptoText, CharsetUtil.CHARSET_UTF_8);
        }
        return cryptoText;
    }
}
