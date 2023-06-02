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

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

/**
 * {@code DurationTypeHandler} 将数据库中的BIGINT映射为Duration，数据库中的单位是秒
 *
 * @author rain
 * @since 2023/4/13 17:30
 */
@MappedTypes({Duration.class})
@MappedJdbcTypes({JdbcType.BIGINT, JdbcType.INTEGER})
public class DurationTypeHandler extends BaseTypeHandler<Duration> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Duration o, JdbcType jdbcType) throws SQLException {
        ps.setLong(i, o.getSeconds());
    }

    @Override
    public Duration getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return Duration.ofSeconds(rs.getLong(columnName));
    }

    @Override
    public Duration getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return Duration.ofSeconds(rs.getLong(columnIndex));
    }

    @Override
    public Duration getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return Duration.ofSeconds(cs.getLong(columnIndex));
    }
}
