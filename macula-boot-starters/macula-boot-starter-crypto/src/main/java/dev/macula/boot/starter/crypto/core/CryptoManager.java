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

package dev.macula.boot.starter.crypto.core;

import cn.hutool.core.util.ReflectUtil;
import dev.macula.boot.starter.crypto.annotation.CryptoField;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 加密管理类
 *
 * @author 老马
 * @version 4.6.0
 */
@Slf4j
public class CryptoManager {

    /**
     * 缓存加密器
     */
    Map<CryptoContext, IEncryptor> encryptorMap = new ConcurrentHashMap<>();

    /**
     * 类加密字段缓存
     */
    Map<Class<?>, Set<Field>> fieldCache = new ConcurrentHashMap<>();

    /**
     * 获取类加密字段缓存
     *
     * @param sourceClazz 源类
     * @return 加密字段集合
     */
    public Set<Field> getFieldCache(Class<?> sourceClazz) {
        return fieldCache.computeIfAbsent(sourceClazz, clazz -> {
            Field[] declaredFields = clazz.getDeclaredFields();
            Set<Field> fieldSet = Arrays.stream(declaredFields)
                .filter(field -> field.isAnnotationPresent(CryptoField.class) && field.getType() == String.class)
                .collect(Collectors.toSet());
            for (Field field : fieldSet) {
                field.setAccessible(true);
            }
            return fieldSet;
        });
    }

    /**
     * 注册加密执行者到缓存
     *
     * @param encryptContext 加密执行者需要的相关配置参数
     * @return 加密执行者实例
     */
    public IEncryptor registerAndGetEncryptor(CryptoContext encryptContext) {
        if (encryptorMap.containsKey(encryptContext)) {
            return encryptorMap.get(encryptContext);
        }
        IEncryptor encryptor = ReflectUtil.newInstance(encryptContext.getAlgorithm().getClazz(), encryptContext);
        encryptorMap.put(encryptContext, encryptor);
        return encryptor;
    }

    /**
     * 移除缓存中的加密执行者
     *
     * @param encryptContext 加密执行者需要的相关配置参数
     */
    public void removeEncryptor(CryptoContext encryptContext) {
        this.encryptorMap.remove(encryptContext);
    }

    /**
     * 根据配置进行加密。会进行本地缓存对应的算法和对应的秘钥信息。
     *
     * @param value          待加密的值
     * @param encryptContext 加密相关的配置信息
     * @return 加密后的字符串
     */
    public String encrypt(String value, CryptoContext encryptContext) {
        IEncryptor encryptor = this.registerAndGetEncryptor(encryptContext);
        return encryptor.encrypt(value, encryptContext.getEncode());
    }

    /**
     * 根据配置进行解密
     *
     * @param value          待解密的值
     * @param encryptContext 加密相关的配置信息
     * @return 解密后的字符串
     */
    public String decrypt(String value, CryptoContext encryptContext) {
        IEncryptor encryptor = this.registerAndGetEncryptor(encryptContext);
        return encryptor.decrypt(value);
    }

}
