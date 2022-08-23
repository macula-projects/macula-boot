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

package dev.macula.boot.starter.database.interceptor;

import cn.hutool.core.util.CharsetUtil;
import dev.macula.boot.starter.database.config.MyBatisPlusProperties;
import dev.macula.boot.starter.database.crypto.AbstractCrypto;
import dev.macula.boot.starter.database.utils.CryptoFieldUtils;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * {@code DecryptInterceptor} 解密拦截器
 *
 * @author rain
 * @since 2022/8/22 11:47
 */
@Intercepts({
        @Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class}),
})
public class DecryptInterceptor extends AbstractCrypto implements Interceptor {

    public DecryptInterceptor(MyBatisPlusProperties properties) {
        super(properties);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        List<Object> result = (List<Object>) invocation.proceed();
        if (result != null && !result.isEmpty()) {
            for (Object obj : result) {
                Set<Field> cryptFields = CryptoFieldUtils.getCryptoFields(obj);
                for (Field cryptField : cryptFields) {
                    Object ciphertext = cryptField.get(obj);
                    if (ciphertext instanceof String && !"".equals(ciphertext)) {
                        cryptField.set(obj, getAES().decryptStr(ciphertext.toString(), CharsetUtil.CHARSET_UTF_8));
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }


}
