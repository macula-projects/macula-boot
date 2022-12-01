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

package dev.macula.boot.starter.mp.interceptor;

/**
 * {@code EncryptInterceptor} mybatis加密字段拦截器
 *
 * @author guhong, rain
 * @since 2022/8/22 11:46
 */

import dev.macula.boot.starter.mp.config.MyBatisPlusProperties;
import dev.macula.boot.starter.mp.crypto.AbstractCrypto;
import dev.macula.boot.starter.mp.utils.CryptoFieldUtils;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.plugin.*;

import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

/**
 * {@code EncryptInterceptor} 加密拦截器
 *
 * @author rain
 * @since 2022/8/22 11:46
 */
@Intercepts({
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
})
public class EncryptInterceptor extends AbstractCrypto implements Interceptor {

    public EncryptInterceptor(MyBatisPlusProperties properties) {
        super(properties);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        StatementHandler statementHandler = (StatementHandler) invocation.getTarget();
        encryptParameters(statementHandler.getBoundSql());
        return invocation.proceed();
    }

    private void encryptParameters(BoundSql boundSql) throws IllegalAccessException {
        // 可以考虑直接在原数据上加密处理，这种方式最简单，弊端是会改变入参的值
        Map<String, String> cryptFieldMap = CryptoFieldUtils.getCryptoMap(null, boundSql.getParameterObject());
        cryptFieldMap.forEach((key, value) -> {
            boundSql.setAdditionalParameter(key, encryptBase64(value));
        });
    }

    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
