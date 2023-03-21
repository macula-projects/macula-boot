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

package dev.macula.boot.starter.mp.interceptor;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import dev.macula.boot.starter.crypto.annotation.CryptoField;
import dev.macula.boot.starter.crypto.config.CryptoProperties;
import dev.macula.boot.starter.crypto.core.CryptoContext;
import dev.macula.boot.starter.crypto.core.CryptoManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.util.*;

/**
 * 入参加密拦截器
 *
 * @author 老马
 * @version 4.6.0
 */
@Slf4j
@Intercepts({@Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class})})
@RequiredArgsConstructor
public class MybatisEncryptInterceptor implements Interceptor {

    private final CryptoManager encryptorManager;
    private final CryptoProperties defaultProperties;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        return invocation;
    }

    @Override
    public Object plugin(Object target) {
        if (target instanceof ParameterHandler) {
            // 进行加密操作
            ParameterHandler parameterHandler = (ParameterHandler)target;
            Object parameterObject = parameterHandler.getParameterObject();
            if (ObjectUtil.isNotNull(parameterObject) && !(parameterObject instanceof String)) {
                this.encryptHandler(parameterObject);
            }
        }
        return target;
    }

    /**
     * 加密对象
     *
     * @param sourceObject 待加密对象
     */
    private void encryptHandler(Object sourceObject) {
        if (ObjectUtil.isNull(sourceObject)) {
            return;
        }
        if (sourceObject instanceof Map<?, ?>) {
            new HashSet<>(((Map<?, ?>)sourceObject).values()).forEach(this::encryptHandler);
            return;
        }
        if (sourceObject instanceof List<?>) {
            List<?> sourceList = (List<?>)sourceObject;
            if (CollectionUtil.isEmpty(sourceList)) {
                return;
            }
            // 判断第一个元素是否含有注解。如果没有直接返回，提高效率
            Object firstItem = sourceList.get(0);
            if (CollectionUtil.isEmpty(encryptorManager.getFieldCache(firstItem.getClass()))) {
                return;
            }
            ((List<?>)sourceObject).forEach(this::encryptHandler);
            return;
        }
        Set<Field> fields = encryptorManager.getFieldCache(sourceObject.getClass());
        try {
            for (Field field : fields) {
                field.set(sourceObject, this.encryptField(String.valueOf(field.get(sourceObject)), field));
            }
        } catch (Exception e) {
            log.error("处理加密字段时出错", e);
        }
    }

    /**
     * 字段值进行加密。通过字段的批注注册新的加密算法
     *
     * @param value 待加密的值
     * @param field 待加密字段
     * @return 加密后结果
     */
    private String encryptField(String value, Field field) {
        CryptoField cryptoField = field.getAnnotation(CryptoField.class);
        CryptoContext encryptContext = new CryptoContext(cryptoField, defaultProperties);

        return this.encryptorManager.encrypt(value, encryptContext);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
