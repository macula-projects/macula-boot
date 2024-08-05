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

package dev.macula.example.gateway.crypto;

import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.SmUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.SM2;
import dev.macula.boot.starter.cloud.gateway.crypto.CryptoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.security.KeyPair;

/**
 * {@code CryptoLocaleServiceImpl} 本地加解密服务
 *
 * @author rain
 * @since 2023/3/23 22:01
 */
@Component
@Slf4j
public class CryptoLocaleServiceImpl implements CryptoService, InitializingBean {
    private SM2 sm2;

    @Override
    public String getSm2PublicKey() {
        return HexUtil.encodeHexStr(sm2.getQ(false));
    }

    @Override
    public String decryptSm4Key(String key) {
        // TODO 缓存解密过后的密钥
        return sm2.decryptStr(key, KeyType.PrivateKey);
    }

    @Override
    public String encrypt(String plainText, String sm4Key) {
        return SmUtil.sm4(HexUtil.decodeHex(sm4Key)).encryptBase64(plainText);
    }

    @Override
    public String decrypt(String secretText, String sm4Key) {
        return SmUtil.sm4(HexUtil.decodeHex(sm4Key)).decryptStr(secretText);
    }

    @Override
    public void afterPropertiesSet() {
        KeyPair pair = SecureUtil.generateKeyPair("SM2");
        sm2 = SmUtil.sm2(pair.getPrivate(), pair.getPublic());
    }
}
