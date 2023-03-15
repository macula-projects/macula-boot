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

package dev.macula.boot.starter.mp.handler;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.symmetric.AES;
import dev.macula.boot.starter.mp.config.MyBatisPlusProperties;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.util.StringUtils;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@code CryptoTypeHandler} 自定义的加密类型处理器
 *
 * @author guhong, rain
 * @since 2022/8/18 17:46
 */
public class CryptoTypeHandler extends BaseTypeHandler<String> {

    private MyBatisPlusProperties properties;
    private AES aes;

    public CryptoTypeHandler(MyBatisPlusProperties properties) {
        this.properties = properties;
    }

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
        throws SQLException {
        if (!"".equals(parameter)) {
            parameter = getAES().encryptBase64(parameter, CharsetUtil.CHARSET_UTF_8);
        }
        ps.setString(i, parameter);
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String columnValue = rs.getString(columnName);
        if (StringUtils.hasLength(columnValue)) {
            columnValue = getAES().decryptStr(columnValue, CharsetUtil.CHARSET_UTF_8);
        }
        return columnValue;
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String columnValue = rs.getString(columnIndex);
        if (StringUtils.hasLength(columnValue)) {
            columnValue = getAES().decryptStr(columnValue, CharsetUtil.CHARSET_UTF_8);
        }
        return columnValue;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String columnValue = cs.getString(columnIndex);
        if (StringUtils.hasLength(columnValue)) {
            columnValue = getAES().decryptStr(columnValue, CharsetUtil.CHARSET_UTF_8);
        }
        return columnValue;
    }

    protected AES getAES() {
        if (aes == null) {
            aes = new AES(properties.getAes().getMode(), properties.getAes().getPadding(),
                properties.getAes().getKey().getBytes(),
                properties.getAes().getIv() == null ? null : properties.getAes().getIv().getBytes());
        }
        return aes;
    }
}
